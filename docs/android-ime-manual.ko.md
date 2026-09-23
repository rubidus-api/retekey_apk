# 안드로이드 IME 구현 매뉴얼

[English](android-ime-manual.md) · **한국어**

안드로이드 입력기(IME)를 만들기 위한 매뉴얼이다. IME가 어떤 구조로 되어 있는지, 그대로 베껴 쓸 수 있는
최소 동작 예제, 읽어 볼 가치가 있는 레퍼런스 구현, 그리고 어려운 부분들 — 에디터 계약, 물리 키, 그리기,
테마 — 을 각각 그것을 빚어낸 실패와 함께 다룬다.

IME 구현에 관한 내용만 담았고, 실제로 배포된 코드를 기준으로 썼다. 여기 있는 규칙은 전부 이 프로젝트가
대가를 치르고 얻은 것이다.

> **이 문서는 살아 있는 문서다.** IME 구현이 바뀔 때마다 — 콜백이 추가되거나, 에디터와의 상호작용이
> 달라지거나, 실기기에서 새로운 함정을 발견할 때마다 — 최신 내용으로 갱신한다. 여기 적힌 동작이 코드와
> 다르다면 틀린 쪽은 이 문서다.
>
> (영어판이 기준 문서이고, 이 문서는 그 번역본이다.)

## 차례

- [1. 안드로이드 IME의 구조](#1-안드로이드-ime의-구조)
  - [1.1 구성 요소](#11-구성-요소)
  - [1.2 시스템이 IME를 찾고, 활성화하고, 선택하는 과정](#12-시스템이-ime를-찾고-활성화하고-선택하는-과정)
  - [1.3 데이터 흐름](#13-데이터-흐름)
  - [1.4 실제 구성 요소 지도](#14-실제-구성-요소-지도)
- [2. 최소 동작 IME](#2-최소-동작-ime)
- [3. 레퍼런스 구현과 받는 곳](#3-레퍼런스-구현과-받는-곳)
- [4. 서비스 생명주기](#4-서비스-생명주기)
- [5. InputConnection 계약](#5-inputconnection-계약)
- [6. 에디터가 권위자다](#6-에디터가-권위자다)
- [7. 에디터 종류와 미상 선택영역](#7-에디터-종류와-미상-선택영역)
- [8. 조합 중인 글자](#8-조합-중인-글자)
- [9. 물리 키보드](#9-물리-키보드)
- [10. 후보 뷰](#10-후보-뷰)
- [11. 커스텀 키보드 그리기](#11-커스텀-키보드-그리기)
  - [11.1 정적인 이미지를 캐시하기](#111-정적인-이미지를-캐시하기)
  - [11.2 터치: 손가락에서 키 입력까지](#112-터치-손가락에서-키-입력까지)
- [12. 테마](#12-테마)
- [12a. 키보드가 사는 창](#12a-키보드가-사는-창)
- [13. 설정과 영속화](#13-설정과-영속화)
- [14. 테스트와 검증](#14-테스트와-검증)
- [15. 안티패턴과 그것을 가르쳐 준 실패들](#15-안티패턴과-그것을-가르쳐-준-실패들)
- [15a. 원격 데스크톱 편집기: 뒤에 편집기가 없는 전선](#15a-원격-데스크톱-편집기-뒤에-편집기가-없는-전선)
- [16. 릴리즈 전 점검표](#16-릴리즈-전-점검표)

## 1. 안드로이드 IME의 구조

### 1.1 구성 요소

IME는 앱 화면이 아니라 **서비스**다. 나머지는 전부 거기에 매달린다.

| 구성 요소 | 정체 | 필수? |
|---|---|---|
| `InputMethodService` 상속 클래스 | IME 본체: 생명주기, 에디터 접근, 키 처리 | 예 |
| 매니페스트 `<service>` 항목 | `BIND_INPUT_METHOD` 권한과 `android.view.InputMethod` 액션으로 선언 | 예 |
| `res/xml/method.xml` | IME 메타데이터: 라벨, 설정 액티비티, 서브타입(언어) | 예 |
| 입력 뷰 | 화면 키보드, `onCreateInputView()`가 반환 | 아니오 — 하드웨어 전용 IME는 없어도 됨 |
| 후보 뷰 | 키보드 위의 막대, `onCreateCandidatesView()` | 아니오 |
| 설정 액티비티 | 평범한 `Activity`, `method.xml`에서 연결 | 아니오, 하지만 있어야 함 |
| 런처 액티비티 | 사용자가 IME를 활성화·선택하도록 돕는 화면 | 아니오, 하지만 있어야 함 |

상대하는 시스템 쪽:

- **`InputConnection`** — 앱의 텍스트에 닿는 유일한 손잡이. 콜백마다 `getCurrentInputConnection()`으로
  얻는다.
- **`EditorInfo`** — 포커스된 필드가 자신에 대해 알려 주는 정보(입력 타입, IME 액션, 초기 선택영역).
  `onStartInput`으로 전달된다.
- **`InputMethodManager`** — 시스템 서비스. 키보드 선택창 띄우기 같은 일에 쓴다.

앱 개발을 하다 온 사람이 놀라는 두 가지:

1. **앱의 `View`를 절대 만지지 않는다.** 텍스트 필드를 직접 읽을 수 없고, 모든 것이 비동기이고 실패할 수
   있는 `InputConnection`을 통한다.
2. **IME 프로세스는 별개이며 오래 산다.** 포커스를 가진 앱에 바인딩되고 앱을 넘나들며 살아남는다. 크래시
   하나가 **시스템 전체**에서 키보드를 앗아간다. [§15.5](#155-예외를-밖으로-흘린-것)가 중요한 이유다.

### 1.2 시스템이 IME를 찾고, 활성화하고, 선택하는 과정

IME는 사용자가 입력하는 모든 것을 볼 수 있으므로, 안드로이드는 두 단계의 명시적 사용자 동작 뒤에 두고
있으며 **앱이 이 둘을 프로그램적으로 대신 수행할 수 없다.**

1. **활성화** — 설정 → *화면 키보드* / *키보드 관리*. 이걸 하기 전까지 시스템 입장에서 그 IME는 존재하지
   않는다.
2. **선택** — 현재 입력기로 지정(키보드 선택창).

대신 *할 수 있는 것*은 사용자를 알맞은 화면으로 보내는 것이다.

```java
// "설치된 키보드 관리": 활성화 단계.
startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

// "키보드 선택": 선택 단계.
InputMethodManager imm = getSystemService(InputMethodManager.class);
if (imm != null) {
    imm.showInputMethodPicker();
}
```

두 가지를 런처 화면에 두어야 한다. 이 두 스위치를 못 찾은 첫 사용자는 키보드가 고장 났다고 결론짓는다.

개발 중에는 셸에서 같은 일을 할 수 있다.

```sh
adb shell ime list -a              # 시스템이 아는 목록
adb shell ime enable  com.example.ime/.MyImeService
adb shell ime set     com.example.ime/.MyImeService
```

### 1.3 데이터 흐름

독립적인 입력 소스가 둘 있고, 서로 다른 곳으로 도착한다.

```
 ┌──────────────┐  터치        ┌───────────────┐
 │   입력 뷰    │────────────▶│               │
 └──────────────┘             │               │   commitText / setComposingText
                              │   내 IME      │   deleteSurroundingText / sendKeyEvent
 ┌──────────────┐  onKeyDown  │   서비스      │──────────────────────────────▶ ┌────────┐
 │  물리 키     │────────────▶│               │                                │ 에디터 │
 └──────────────┘             │               │◀────────────────────────────── └────────┘
                              └───────────────┘   onStartInput(EditorInfo)
                                                  onUpdateSelection(...)
```

- **터치**는 내 뷰로 온다. 그 의미를 내가 정하고 `InputConnection`을 호출한다.
- **물리 키**는 **앱보다 먼저** `onKeyDown`/`onKeyUp`으로 온다. `true`를 반환하면 키를 소비하고,
  `super.onKeyDown(...)`을 반환하면 앱이 받는다([§9](#9-물리-키보드)).
- **에디터가 말을 걸어오는 것**은 `onStartInput`(어떤 종류의 필드인지)과 `onUpdateSelection`(커서가
  움직였다) 뿐이다. 나머지는 물어봐야 하고, 못 받을 수도 있다.

그래서 실제 IME에서 소프트 키 한 번의 여정은 이렇다.

```
터치 → 키 히트테스트 → 의미 이벤트("자모 ㄱ", "백스페이스", "원시 F5")
     → 조합기 / 디스패처 (상태를 가질 수 있음)
     → 에디터 동작 목록
     → InputConnection 호출
     → (나중에, 어쩌면) onUpdateSelection
```

이 단계를 분리해 두는 것이 IME를 테스트 가능하게 만든다. "InputConnection 호출" 이전은 전부 순수 자바로
둘 수 있기 때문이다([§14](#14-테스트와-검증)).

### 1.4 실제 구성 요소 지도

위 분리의 구체적 사례로서 ReteKey의 구조다. 이름은 예시일 뿐이고, 형태가 요점이다.

| 계층 | 책임 | 안드로이드 비의존? |
|---|---|---|
| `ReteKeyImeService` | 생명주기, `InputConnection` 호출, 물리 키, 후보 | 아니오 |
| `ReteKeyboardView` | 키보드 그리기, 터치 히트테스트, 의미 이벤트 방출 | 아니오 |
| `KeyboardPalette` | 시스템·Material You 테마에서 색 해석 | 아니오 |
| `KeyboardLayouts` / `SoftwareKeySpec` | 어떤 키가 어디에 있고 무슨 뜻인지 | **예** |
| `HangulComposer` / `HangulInputProcessor` | 2벌식 오토마타와 그 에디터 동작 | **예** |
| `InputDispatcher` / `TransitionPlan` / `KeyAction` | 이벤트를 순서 있는 편집 목록으로 | **예** |
| `CheckedEditorExecutor` | 편집을 실행하고 결과를 보고 | **예** |
| `InputConnectionEditorBridge` | `InputConnection`을 만지는 유일한 곳 | 아니오 |
| `InputSessionController` | 수동적 커서 캐시([§6](#6-에디터가-권위자다)) | **예** |
| `HanjaTable` / `HunumTable` / `HanjaDictionary` | 변환 데이터와 조회 | 테이블 로직은 **예** |
| `SettingsActivity` / `PreviewActivity` | 설정과 활성화·선택 도우미 화면 | 아니오 |

유용한 것은 "안드로이드 비의존?" 열이다. **IME의 대략 70%는 순수 자바로 만들 수 있고**, 로직 버그는 바로
거기에 산다.

## 2. 최소 동작 IME

아래는 완전히 동작하는 IME다. 버튼 하나짜리 키보드가 뜨고 "A"를 입력한다. 여기서 시작해 키워 나가면 된다.

**`AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="@string/app_name">
        <service
            android:name=".MyImeService"
            android:label="@string/ime_name"
            android:permission="android.permission.BIND_INPUT_METHOD"
            android:exported="true">
            <intent-filter>
                <action android:name="android.view.InputMethod" />
            </intent-filter>
            <meta-data
                android:name="android.view.im"
                android:resource="@xml/method" />
        </service>
    </application>
</manifest>
```

`android:permission="android.permission.BIND_INPUT_METHOD"`는 시스템만 이 서비스를 바인딩할 수 있게
한다. 이것이 없거나 `android.view.InputMethod` 액션이 정확하지 않으면, **아무 오류 메시지 없이** 키보드
목록에 나타나지 않는다.

**`res/xml/method.xml`**

```xml
<input-method xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.example.ime.SettingsActivity"
    android:supportsSwitchingToNextInputMethod="true">
    <subtype
        android:label="@string/subtype_korean"
        android:imeSubtypeLocale="ko_KR"
        android:imeSubtypeMode="keyboard"
        android:isAsciiCapable="false" />
    <subtype
        android:label="@string/subtype_english"
        android:imeSubtypeLocale="en_US"
        android:imeSubtypeMode="keyboard"
        android:isAsciiCapable="true" />
</input-method>
```

서브타입은 시스템이 내 IME가 어떤 언어를 제공하는지 아는 수단이며, 지구본 키와
`onCurrentInputMethodSubtypeChanged`를 움직인다. 최소 하나는 `isAsciiCapable`로 표시할 것. 그러지 않으면
일부 비밀번호 필드가 내 IME를 거부한다.

**서비스**

```java
public class MyImeService extends InputMethodService {

    @Override
    public View onCreateInputView() {
        Button key = new Button(this);
        key.setText("A");
        key.setOnClickListener(v -> commit("A"));
        return key;                       // 어떤 View든 키보드가 될 수 있다
    }

    private void commit(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {                 // 언제든 가능한 일이다
            return;
        }
        ic.commitText(text, 1);
    }
}
```

이것으로 동작하는 입력기가 된다. 이후의 모든 것은 그것을 *잘* 하는 문제다. 조합, 물리 키, 터미널을
망가뜨리지 않기, 그리고 크래시하지 않기.

**키워 나가기.** 가장 덜 아픈 순서로 다음 세 단계:

1. `Button`을 키 격자를 히트테스트하고 `InputConnection`을 직접 부르는 대신 *의미* 이벤트
   (`"자모 ㄱ"`, `"백스페이스"`)를 방출하는 커스텀 `View`로 교체한다.
2. 그 이벤트와 실제 편집 사이에 순수 자바 계층을 넣어 단위 테스트할 수 있게 한다.
3. 그 다음에야 조합기, 물리 키, 후보를 추가한다.

## 3. 레퍼런스 구현과 받는 곳

무언가를 발명하기 전에 이것들을 읽을 것. 앞의 둘은 이 프로젝트가 실제로 기댄 것이다.

> **베끼기 전에 라이선스부터.** AOSP는 LatinIME도 `SoftKeyboard` 예제도 **Apache-2.0**이지 MIT가
> 아니다. 읽고 아이디어를 다시 구현하는 데는 의무가 붙지 않으며, 이 프로젝트가 하는 일이 딱 그것이다.
> 파일을 붙여 넣는 순간 달라진다. Apache 헤더를 그대로 두어야 하고, 라이선스 전문을 앱과 함께
> 배포해야 하며, 고친 파일에는 변경 표시를 남겨야 하고, README에서 "앱 전체가 내 라이선스"라고 더는
> 말할 수 없다. [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) 참고.

**AOSP LatinIME** — Google 키보드 계열이자 안드로이드에서 가장 완성도 높은 공개 IME이며,
[§6](#6-에디터가-권위자다)의 커서 모델이 여기서 왔다.

```sh
git clone https://android.googlesource.com/platform/packages/inputmethods/LatinIME
```

읽을 가치가 있는 것: `LatinIME.java`(서비스), 그리고 `RichInputConnection.java` — 그 안의
`mExpectedSelStart` / `mExpectedSelEnd` 쌍이 바로 수동적 커서 캐시이고, 주석이 에디터가 얼마나 자주
다르게 말하는지 솔직하게 적어 두었다.

**AOSP `SoftKeyboard` 예제** — "골격은 이렇다"를 보여 주는 작은 공식 IME. LatinIME보다 처음부터 끝까지
읽기 훨씬 쉽고, §2의 템플릿으로 알맞다.

```sh
git clone https://android.googlesource.com/platform/development
# samples/SoftKeyboard/
```

**공식 문서** (경로가 자주 바뀌므로 제목도 함께 적는다):

- *Create an input method* — 플랫폼 가이드:
  <https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method>
- `InputMethodService`:
  <https://developer.android.com/reference/android/inputmethodservice/InputMethodService>
- `InputConnection`:
  <https://developer.android.com/reference/android/view/inputmethod/InputConnection>
- `EditorInfo`:
  <https://developer.android.com/reference/android/view/inputmethod/EditorInfo>
- `InputMethodManager`:
  <https://developer.android.com/reference/android/view/inputmethod/InputMethodManager>

**효과적인 읽기 순서:** 용어를 익히러 플랫폼 가이드 → 골격을 보러 `SoftKeyboard` → `InputConnection`
레퍼런스 페이지 전체(짧고 모든 문단이 중요하다) → 어려운 부분을 보러 LatinIME.

**이 프로젝트.** ReteKey 자체가 MIT 라이선스이고 처음부터 끝까지 읽을 만하다. 한글 오토마타와 한자
테이블은 형제 프로젝트에서 이식했고 `THIRD_PARTY_NOTICES.md`에 출처를 밝혀 두었다. 재사용하고 싶은
데이터의 라이선스를 확인할 곳도 거기다.

## 4. 서비스 생명주기

실제로 구현하게 되는 콜백:

| 콜백 | 시점 | 용도 |
|---|---|---|
| `onCreate` | 서비스 생성 | 프로세스 단위 초기화 |
| `onCreateInputView` | 키보드 뷰가 처음 필요할 때 | 뷰 생성, 콜백 연결 |
| `onCreateCandidatesView` | 후보 막대가 처음 표시될 때 | 후보 UI 생성 |
| `onStartInput(info, restarting)` | 새 에디터가 붙을 때 | 에디터 분류, 상태 초기화 |
| `onStartInputView(info, restarting)` | 키보드가 보이게 될 때 | 일시적 UI 상태 초기화 |
| `onUpdateSelection(...)` | 에디터 커서가 움직일 때 | 커서 캐시 갱신 |
| `onFinishInputView(finishing)` | 키보드가 숨겨질 때 | 조합 마무리 |
| `onFinishInput` | 에디터가 떨어질 때 | 조합 마무리, 세션 종료 |
| `onUnbindInput` | 입력 바인딩 해제 | 세션 종료 |
| `onDestroy` | 서비스 소멸 | 자원 해제 |

그 밖에 쓸모 있는 것:

- `onEvaluateInputViewShown()` — `false`를 반환하면 화면 키보드를 숨긴다(예: 물리 키보드가 붙었을 때).
  무시하지 말고 `super`를 호출해 결합할 것.
- `onEvaluateFullscreenMode()` — 가로 전체화면 추출 모드를 정말 원하는 게 아니라면 `false`.
- `onCurrentInputMethodSubtypeChanged(subtype)` — 언어가 바뀌었다. 조합을 마무리할
  것([§8](#8-조합-중인-글자)).

**문서에 나오는 깔끔한 순서를 가정하지 말 것.** API 33에서 다른 IME로 전환할 때 실측한 순서:

```
onUnbindInput → (onDestroy 시작) → onFinishInputView → onFinishInput → (onDestroy 종료)
```

즉 **`onUnbindInput`이 `onFinishInput`보다 먼저** 오고, finish 계열 콜백은 `onDestroy` **안에 중첩**된다.
`onUnbindInput`에서 세션 표식을 지우는 코드는 자기 teardown 콜백을 귀속시키지 못하게 된다. teardown은
멱등하게 짤 것: 어느 것이 먼저 올 수도, 두 번 올 수도, 이미 입력 연결이 사라진 뒤일 수도 있다.

규칙:

- 모든 teardown 경로는 두 번 실행돼도 안전해야 한다.
- `getCurrentInputConnection()`은 teardown 중을 포함해 언제든 `null`일 수 있다. 항상 null 검사하고,
  콜백 사이에 연결을 캐시하지 말 것.
- 조합 마무리는 `onFinishInput`, `onFinishInputView`, `onCurrentInputMethodSubtypeChanged`
  **전부**에서 할 것.

## 5. InputConnection 계약

에디터에 가하는 모든 동작은 `getCurrentInputConnection()`을 통한다.

| 호출 | 의미 | 비고 |
|---|---|---|
| `commitText(text, 1)` | 커서 위치에 텍스트 삽입 | 선택영역이 있으면 대체 |
| `setComposingText(text, 1)` | 밑줄 조합(preedit) 표시 | 이전 조합영역을 대체 |
| `finishComposingText()` | 조합을 확정 | 조합 중이 아니면 무동작 |
| `deleteSurroundingText(before, after)` | 커서 기준 상대 삭제 | **커서 위치를 몰라도 된다** |
| `deleteSurroundingTextInCodePoints(b, a)` | 위와 같고 유니코드 안전 | 사용자 기준 글자 삭제엔 이쪽 |
| `sendKeyEvent(event)` | 실제 `KeyEvent` 전달 | 조합키·원시키를 보내는 유일한 수단 |
| `performEditorAction(id)` | Go/Search/Send 실행 | `EditorInfo.imeOptions` 기반 |
| `performContextMenuAction(id)` | 전체선택/복사/붙여넣기/실행취소 | 리치 에디터에서만, 터미널은 무시 |
| `getTextBeforeCursor(n, 0)` | 텍스트 되읽기 | `null`이거나 잘릴 수 있음 |
| `getSelectedText(0)` | 선택영역 읽기 | `null`이거나 예외를 던지는 에디터도 있음 |
| `beginBatchEdit()` / `endBatchEdit()` | 편집 묶기 | 삭제 후 커밋을 한 번의 변경으로 |

나머지보다 중요한 두 가지 성질:

1. **삽입과 상대 삭제는 커서 기준이다.** `commitText`, `setComposingText`, `deleteSurroundingText`는
   모두 *에디터의* 커서에서 동작한다. 타이핑이나 백스페이스를 위해 절대 선택영역을 알 필요가 없다.
2. **모든 호출은 실패하거나 무시될 수 있다.** `boolean`을 돌려주고 예외도 던진다. `false`는 "내 상태가
   깨졌다"가 아니라 "이 에디터는 그 동작을 하지 않았다"로 해석할 것.

읽기를 변환 결과로 바꾸는 것은 하나의 배치다:

```java
ic.beginBatchEdit();
try {
    if (deleteLength > 0) {
        ic.deleteSurroundingText(deleteLength, 0);
    }
    ic.commitText(replacement, 1);   // 선택영역이 살아 있으면 이것이 대체가 된다
} finally {
    ic.endBatchEdit();
}
```

실제 키 보내기(더 나은 매핑이 없는 의미 키의 폴백):

```java
private void sendRawKey(int keyCode, int metaState) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) {
        return;
    }
    long now = SystemClock.uptimeMillis();
    ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState,
        KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD));
    ic.sendKeyEvent(new KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0,
        metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD));
}
```

`ACTION_DOWN`과 `ACTION_UP`을 항상 쌍으로 보내고, 둘이 같은 `downTime`을 쓰게 할 것. 그러지 않으면 대상
앱의 반복 감지가 이상해진다.

## 6. 에디터가 권위자다

가장 중요한 설계 규칙이자, 가장 비싸게 배운 규칙이다.

**텍스트와 커서의 주인은 에디터다. IME가 아는 값은 언제든 틀릴 수 있는 힌트일 뿐이다.** 선택 갱신은
늦게, 순서가 뒤바뀌어, 합쳐져서 오거나 아예 오지 않는다. `-1`을 보고하는 에디터도 있고, 예측과 다르게
편집을 적용하는 에디터도 있다.

따라서:

- **수동적 커서 캐시**를 둘 것: 자기 편집 후에는 낙관적으로 갱신하고, `onUpdateSelection`이 오면
  무조건 덮어쓰게 한다. AOSP LatinIME 모델이다
  (`RichInputConnection.mExpectedSelStart` / `mExpectedSelEnd`).
- 캐시가 에디터와 어긋난다는 이유로 입력을 거부하는 상태에 **절대** 들어가지 말 것. "동기화 깨짐" 상태
  자체가 있어선 안 된다. 화해가 안 되면 에디터가 보고한 값을 채택하고 계속 간다.
- 미상을 허용할 것: `-1` 선택영역은 오류가 아니라 정상이다.

```java
// 모델 전체가 이것이다. 확인도, 예약도, 실패 상태도 없다.
@Override
public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                              int newSelStart, int newSelEnd,
                              int candidatesStart, int candidatesEnd) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
        candidatesStart, candidatesEnd);
    if (newSelStart < 0 || newSelEnd < 0) {
        cursor = EditorBounds.unknown();          // 미상은 정상 상태다
        return;
    }
    cursor = EditorBounds.of(newSelStart, newSelEnd, candidatesStart, candidatesEnd);
}

// 자기 편집 뒤에는 추측해도 되지만, 그 추측에는 권위가 없다.
private void afterOwnEdit(EditorBounds predicted) {
    cursor = predicted;    // 다음 onUpdateSelection이 무엇을 말하든 덮어쓴다
}
```

반대로 했을 때 무슨 일이 벌어지는지는
[§15.1](#151-빠져나올-수-없는-엄격한-기대-원장)을 볼 것.

## 7. 에디터 종류와 미상 선택영역

에디터는 `onStartInput`에서 `EditorInfo`로 한 번 분류한다.

```java
@Override
public void onStartInput(EditorInfo info, boolean restarting) {
    super.onStartInput(info, restarting);
    boolean rawKeyEditor = info != null
        && (info.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_NULL;
    profile = rawKeyEditor ? EditorProfile.rawKeys() : EditorProfile.richText();
    cursor = (info != null && info.initialSelStart >= 0)
        ? EditorBounds.of(info.initialSelStart, info.initialSelEnd, -1, -1)
        : EditorBounds.unknown();     // 터미널이 여기로 온다
}
```

- `TYPE_NULL` — 이 에디터는 텍스트 편집이 아니라 **원시 키 이벤트**를 원한다. 터미널 에뮬레이터, 일부
  게임/콘솔 뷰가 그렇다. `sendKeyEvent`로 키를 보낸다.
- 그 외는 리치 텍스트 에디터다. `commitText` / `setComposingText`가 적절하다.

터미널은 추가로 `initialSelStart == -1`을 보고하고 의미 있는 `onUpdateSelection`을 보내지 않는 경우가
많다. **선택영역을 알아야만 하는 키보드는 터미널에서 타이핑할 수 없다.**

규칙:

- 삽입이나 백스페이스를 "선택영역을 안다"는 조건으로 막지 말 것.
- `performContextMenuAction`이 동작한다고 가정하지 말 것. 터미널은 무시한다.
- 에디터에 액션이 있다고 가정하지 말 것. 액션이 없고 한 줄짜리라면 실제 `KEYCODE_ENTER`가 올바른
  폴백이다([§15.3](#153-원시-엔터를-거부한-것)).

엔터 키의 의미는 `EditorInfo.imeOptions`에서 읽는다.

```java
int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
boolean noEnterAction = (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
boolean multiLine = (info.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;

if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE) {
    ic.performEditorAction(action);          // Go / Search / Send / Done
} else if (multiLine) {
    ic.commitText("\n", 1);
} else {
    sendRawKey(KeyEvent.KEYCODE_ENTER, 0);   // 아무것도 안 하는 선택지는 없다
}
```

## 8. 조합 중인 글자

상태를 가진 조합기(한글, 병음, 가나)는 진행 중인 음절을 `setComposingText`로 보여 주고
`finishComposingText`로, 혹은 다음 음절을 커밋하며 확정한다.

조합기의 한 단계는 호출 하나가 아니라 *순서 있는 편집 목록*을 만든다. 이 부분을 명시적으로 모델링할
가치가 있다. 정확히 여기가 단위 테스트 가능한 부분이기 때문이다.

```java
// "ㄱ" 다음 "ㅏ" 다음 "ㄴ" 다음 "ㄷ": 가 → 간 → commit("가") + compose("ㄴㄷ"→ 낟) …
List<KeyAction> actions = composer.accept(jamo);
for (KeyAction action : actions) {
    switch (action.kind()) {
        case COMMIT_TEXT:        ic.commitText(action.text(), 1);        break;
        case SET_COMPOSING_TEXT: ic.setComposingText(action.text(), 1);  break;
        case DELETE_BACKWARD:    ic.deleteSurroundingTextInCodePoints(1, 0); break;
        case RAW_KEY:            sendRawKey(action.keyCode(), action.meta()); break;
    }
}
```

**세션 경계마다 조합을 마무리할 것.** 사용자가 음절 도중에 필드를 떠나면 미완성 preedit이 남는다 —
어느 세션에도 속하지 않는 밑줄 텍스트가 남아 나중에 다시 나타나거나 예상치 못하게 지워진다.

```java
@Override public void onFinishInput()      { finishComposingInEditor(); /* … */ }
@Override public void onFinishInputView(boolean finishing) { finishComposingInEditor(); reset(); }
@Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype s) {
    finishComposingInEditor();   // 언어 전환이 반쯤 만들어진 자모를 넘겨선 안 된다
    reset();
}

private void finishComposingInEditor() {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
        ic.finishComposingText();
    }
}
```

조합 중인 글자를 평범한 텍스트로 다뤄야 할 때 — 예를 들어 한자로 변환할 때 — 가장 단순하고 올바른
방법은 먼저 `finishComposingText()`로 확정한 뒤 `getTextBeforeCursor`로 되읽어 교체하는 것이다. 조합
영역을 직접 모델링하지 않아도 된다.

## 9. 물리 키보드

물리 키는 애플리케이션보다 **먼저** `onKeyDown` / `onKeyUp` / `onKeyMultiple`으로 온다. `true`를
반환한 키는 앱이 영영 받지 못한다.

**수정자 조합은 통과시킬 것.** 수정자 키 자체와 Ctrl / Alt / Meta가 걸린 모든 조합은 애플리케이션
단축키다. IME가 삼키면 Ctrl+A / Ctrl+C / Ctrl+V가 모든 앱에서 죽는다.

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    // 1. 사용자가 IME 기능에 바인딩한 키는 통과 검사보다 반드시 먼저 확인해야 한다.
    //    그러지 않으면 오른쪽 Ctrl 같은 바인딩은 보기도 전에 앱으로 위임된다.
    if (event.getRepeatCount() == 0 && handleBoundFunctionKey(keyCode, event)) {
        return true;
    }
    // 2. 수정자 키와 Ctrl/Alt/Meta 조합은 앱의 것이다.
    if (KeyEvent.isModifierKey(keyCode) || event.isCtrlPressed()
            || event.isAltPressed() || event.isMetaPressed()) {
        return super.onKeyDown(keyCode, event);
    }
    // 3. 내 처리(자모 매핑, 원시 키 등).
    return handle(keyCode, event) || super.onKeyDown(keyCode, event);
}
```

Shift는 의도적으로 통과 검사에서 뺐다. Shift+글자는 IME가 그대로 조합하는 평범한 텍스트이고, 프레임워크가
어차피 글자 이벤트에 shift 메타 상태를 실어 준다.

반대로 조합키를 *직접 보낼* 때(소프트 Ctrl, 리맵된 키)는 메타 상태를 담은 실제 `KeyEvent`를 만들어
`sendKeyEvent`로 보낸다. 양쪽 모두에서 통한다. 리치 에디터는 `onKeyShortcut`으로 Ctrl+A를 전체선택으로
처리하고, 터미널은 제어 코드를 받는다.

```java
sendRawKey(KeyEvent.KEYCODE_B, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
```

**물리 키보드에서 누르고 있는 수정자는 화면 키에도 걸린다.** 한 손으로 키보드의 Ctrl 을 누른 채 다른
손으로 액션바의 방향키를 누를 수 있다. 그 방향키는 키보드 자신의 방향키가 그렇듯 Ctrl+방향키로
나가야 한다. IME 는 수정자 키의 누름과 뗌을 모두 보므로 그것을 기록해 두면 된다 — 키코드마다 따로,
그래야 오른쪽 Shift 를 누른 채 왼쪽 Shift 를 떼도 Shift 가 눌린 것으로 남는다. 기록은
`onKeyDown`·`onKeyUp` 의 **첫 줄에서**, 어떤 조기 반환보다 먼저 해야 한다. 그러지 않으면 그대로
넘기는 경로가 뗌을 놓친다.

```java
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    heldHardware.onKey(keyCode, true);          // 무엇이 반환하기 전에
    ...
}

// 화면 키의 수정자: 소프트 래치, 액션바의 것, 물리 키보드의 것.
Set<KeyModifier> mods = union(keyboardView.rawKeyModifiers(), heldHardware.held());
```

그 집합은 **손가락이 닿는 순간 한 번** 읽어서 누르고 있는 동안 내내 쓸 것. 누르고 있으면 반복되는
키가 그러지 않으면 첫 반복에서 한 번짜리 Ctrl 을 잃는다 — 래치는 첫 키 이벤트에서 소진되고 — 나머지
반복은 수정자 없이 나간다(§15.32).

**소프트 수정자는 키가 치는 글자가 아니라 키의 자리로 조합한다.** 물리 한글 키보드에서 Ctrl+C 는 ㅊ
을 치는 키이고, 사람들은 그 키를 누른다. 자모를 치는 화면 페이지는 조합을 만들기 전에 키를 같은
자리의 영문자로 바꿔야 한다. 그러지 않으면 한글 페이지에서 Ctrl 을 켜고 누른 키가 복사 대신 ㅊ 을
친다 — 모든 앱에서. ReteKey 는 그 표를 자판 곁에 두고(`ChordLetters`), 글자 키 26개가 서로 다른 26개
글자로 가는지 단위 테스트가 확인한다.

좌우 수정자는 서로 다른 키코드(`KEYCODE_CTRL_LEFT`=113, `KEYCODE_CTRL_RIGHT`=114)이며, 이 점이
"오른쪽 Ctrl은 한/영, 왼쪽 Ctrl은 그대로 Ctrl+C"를 가능하게 한다.

수정자 단독 바인딩은 **키를 뗄 때** 캡처할 것. 누르는 시점에는 Ctrl 단독인지 Ctrl+Space의 시작인지
구분할 수 없다.

```java
// 설정 액티비티에서 사용자 지정 단축키 캡처하기.
@Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (!capturing) return super.onKeyDown(keyCode, event);
    if (KeyEvent.isModifierKey(keyCode)) return true;         // 대기: 조합인가, 단독 수정자인가?
    save(new Binding(modifiersOf(event), keyCode));           // 예: Shift+Space
    capturing = false;
    return true;
}
@Override public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (capturing && KeyEvent.isModifierKey(keyCode)) {
        save(new Binding(0, keyCode));                        // 예: 오른쪽 Ctrl 단독
        capturing = false;
        return true;
    }
    return super.onKeyUp(keyCode, event);
}
```

끝으로, 물리 키보드가 붙어 있을 때 화면 키보드를 숨길지 정한다.

```java
@Override
public boolean onEvaluateInputViewShown() {
    super.onEvaluateInputViewShown();
    Configuration config = getResources().getConfiguration();
    boolean hardware = config.keyboard != Configuration.KEYBOARD_NOKEYS
        && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
    return !hardware;
}
```

## 10. 후보 뷰

`onCreateCandidatesView()`와 `setCandidatesViewShown(true/false)`는 키보드 뷰와 독립적인 막대를
제공한다. 이것이 중요한 이유는 **입력 뷰가 숨겨져 있어도 후보 뷰는 표시되기 때문**이다 — 물리 키보드가
붙어 있을 때가 정확히 그 상황이다. 변환 UI는 키보드 뷰가 아니라 여기에 두어야 한다.

뷰는 지연 생성되므로 순서를 지킬 것:

```java
@Override
public View onCreateCandidatesView() {
    candidatesView = new CandidatesView(this);
    candidatesView.setOnPick(this::commitCandidate);
    if (pendingItems != null) {                 // 뷰가 생기기 전에 표시 요청이 온 경우
        candidatesView.show(pendingReading, pendingItems);
    }
    return candidatesView;
}

private void showCandidates(String reading, List<Item> items) {
    pendingReading = reading;
    pendingItems = items;
    setCandidatesViewShown(true);               // 여기서 onCreateCandidatesView가 불릴 수 있다
    if (candidatesView != null) {
        candidatesView.show(reading, items);
    }
    candidatesShown = true;
}
```

사용자가 다른 키를 누르면, 그리고 `onStartInput` / `onFinishInputView`에서 막대를 숨길 것. 그러지
않으면 참조하던 텍스트보다 오래 살아남는다. 숫자키 선택을 지원한다면, "다른 키를 누르면 막대를 숨긴다"
규칙보다 *먼저* 그 키들을 가로챌 것.

아무도 볼 수 없던 띠로 이 프로젝트가 치른 규칙 넷(이슈 #7, 0.1.170):

- **띠를 켜는 것만으로는 보이지 않는다.** `InputMethodService` 는 candidates 틀을 *전체 화면 영역*
  안에 두고, 그 영역의 가시성을 창이 배치되는 순간의 candidates 가시성으로 맞춘다. 나중의
  `setCandidatesViewShown(true)` 는 안쪽 틀만 바꾸므로, 키가 뜬 뒤에 켠 띠는 켜진 채 글자까지 담고
  숨은 부모 안에 있다. 띠를 보일 때 자기 뷰에서 위로 올라가며 숨은 조상을 모두 보이게 할 것.
- **`getCandidatesHiddenVisibility()` 에서 `View.GONE` 을 돌려줄 것.** 기본값 `INVISIBLE` 은 띠
  높이를 차지한다. candidates 뷰는 그것을 쓰는 편집기가 아니라 창 전체에 있으므로, 모든 앱에서 키
  위에 빈 줄이 생긴다.
- **`setCandidatesViewShown` 은 상태가 바뀔 때만 부를 것.** 입력 뷰가 요청되지 않은 상태에서
  candidates 뷰를 숨기면 창 전체가 숨어, 그 창이 보여 주던 다른 패널 — 물리 키보드와 함께 쓰는 한자
  목록 같은 — 까지 내려간다.
- **떠 있는 키보드는 이것을 쓸 수 없다.** 떠 있는 패널의 창은 화면 전체를 덮으므로 띠는 화면 맨 위
  상태 표시줄 밑에 붙고, 나타날 때마다 아래 패널이 그 높이만큼 줄어 키 크기가 바뀐다. 글자는 패널
  자신에 그릴 것 — ReteKey 는 패널의 제목 줄에 그린다.

```java
private void revealCandidatesArea() {
    View decor = getWindow().getWindow().getDecorView();
    ViewParent parent = strip.getParent();
    while (parent instanceof View && parent != decor) {
        View area = (View) parent;
        if (area.getVisibility() != View.VISIBLE) area.setVisibility(View.VISIBLE);
        parent = area.getParent();
    }
}

@Override public int getCandidatesHiddenVisibility() { return View.GONE; }
```

헤드리스 에뮬레이터의 화면에는 이 중 아무것도 나오지 않는다. 뷰 트리에는 나온다. IME 창의 decor 뷰를
각 뷰의 가시성·크기·위치와 함께 찍으면(§14), 보이는 띠 위의 `I` 로 표시된 부모 하나가 결함의 전부다.

## 11. 커스텀 키보드 그리기

캔버스로 그리는 키보드는 키를 누를 때마다 다시 그린다. 문제가 되는 비용은
*다시 그리는 빈도 × 한 번 그리는 복잡도*이므로, 프레임당 작업량을 바뀐 것에 비례하게 유지해야 한다.

### 11.1 정적인 이미지를 캐시하기

**정적인 키보드를 캐시할 것.** 눌리지 않은 키보드 — 키 모양, 입체감, 라벨 — 를 `Bitmap`에 한 번 렌더한
뒤, 매 프레임 그 비트맵을 붙이고 눌린 키의 오버레이만 그린다.

```java
@Override
protected void onDraw(Canvas canvas) {
    int width = getWidth();
    int height = getHeight();
    if (width <= 0 || height <= 0) {
        return;
    }
    ensureBaseBitmap(width, height);              // 시그니처가 바뀔 때만 재생성
    canvas.drawBitmap(baseBitmap, 0f, 0f, null);
    drawPressFeedback(canvas, width, height);     // 둥근 사각형 하나
}

private void ensureBaseBitmap(int width, int height) {
    String signature = layoutSignature();
    if (baseBitmap != null && signature.equals(baseSignature)
        && baseBitmap.getWidth() == width && baseBitmap.getHeight() == height) {
        return;                                   // 흔한 경우: 재사용
    }
    if (baseBitmap != null) {
        baseBitmap.recycle();
    }
    baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas cache = new Canvas(baseBitmap);
    cache.drawColor(palette.background);
    for (Key key : layout.keys()) {
        drawKey(cache, key);                      // 둥근 면 + 그림자 립 + 라벨
    }
    baseSignature = signature;
}

/** 정적 이미지가 의존하는 모든 것 — 그리고 의도적으로 눌린 키는 넣지 않는다. */
private String layoutSignature() {
    return page + "|" + layoutId + "|" + numpadMode + "|" + shift.isActive()
        + "|" + shift.isLocked() + "|" + armedModifiers + "|" + isNightMode();
}
```

입체감 자체는 둥근 사각형 두 개면 되고, 캐시에 구워 넣을 만큼 싸다.

```java
paint.setColor(palette.keyShadow);                                    // 면 아래의 립
canvas.drawRoundRect(l, t + shadowPx, r, b + shadowPx, radius, radius, paint);
paint.setColor(keyFillColor(key));                                    // 그 위에 면
canvas.drawRoundRect(l, t, r, b, radius, radius, paint);
```

그 밖의 규칙:

- `onDraw`에서 아무것도 할당하지 말 것 — `Paint`도, `Shader`도, 박싱도. 필드를 재사용한다.
- 입체감은 흐림 처리로 비싼 `setShadowLayer`보다 1–2px 어긋난 둥근 사각형으로 낼 것.
- `onDetachedFromWindow`에서 캐시 비트맵을 recycle할 것.

### 11.2 터치: 손가락에서 키 입력까지

터치 층이 답하는 질문은 셋이고, 각각에는 버그가 아니라 **고장 난 자판처럼 느껴지는** 오답이 있다.
**어느 키인가**, **누구의 손가락인가**, **언제 발사되는가**.

**어느 키인가: 모든 픽셀이 어느 키엔가 속한다.** 키는 작은 간격만큼 안으로 들여 그려지므로, 경계 근처
탭이 옆 키에 닿지 않도록 그 *보이는 면*과 비교하고 싶어진다. 그러지 말 것. 간격은 그림의 것이지 목표물의
것이 아니다 — 히트 박스를 들여놓으면 키 쌍마다 죽은 띠가 생기고, 240dpi 기기에서 그 띠는 자판 넓이의
3분의 1이 아무 답도 하지 않는다는 뜻이다(§15.16). 그리기가 쓰는 것과 같은 경계로, 격자 칸 전체에 대해
판정할 것.

```java
private int rowAt(KeyboardLayout layout, float y) {
    int height = getHeight();
    if (height <= 0 || y < 0.0f || y >= height) {
        return -1;
    }
    return Math.min(layout.rows().size() - 1, (int) (y * layout.rows().size() / height));
}
```

**누구의 손가락인가: 포인터마다 자기 키를 갖는다.** 타이핑은 겹친다 — 앞 손가락이 떨어지기 전에 다음
손가락이 내려앉는다 — 그래서 `ACTION_DOWN` 과 `ACTION_UP` 만 처리하는 뷰는 그 사이에 눌린 것을 전부
잃고, 마지막으로 뗄 때 **첫** 손가락의 키를 놓아 준다(§15.11). 마스킹된 동작을 처리하고, 상태를 포인터
id 로 색인할 것.

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
    int index = event.getActionIndex();
    switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            beginTouch(event.getPointerId(index), event.getX(index), event.getY(index));
            return true;
        case MotionEvent.ACTION_MOVE:
            moveTouches(event);                 // getActionIndex() 가 아니라 포인터 전부
            return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            endTouch(event.getPointerId(index));
            return true;
        case MotionEvent.ACTION_CANCEL:
            cancelAllTouches();
            return true;
        default:
            return true;
    }
}
```

누름이 지니는 것은 전부 뷰가 아니라 손가락의 것이다 — 키, 길게 누르기 타이머, 자동 반복 타이머,
"홀드가 이미 처리했다" 표시, 그리고 그 색인을 읽어 온 격자까지.

```java
private final SparseArray<Touch> touches = new SparseArray<>();

private final class Touch {
    final int pointerId;
    final String grid;            // 누르는 도중 다른 손가락이 페이지를 바꿀 수 있다
    int row;                      // final 이 아니다: 미끄러진 손가락은 옮겨간 키를 갖는다
    int key;
    boolean holdConsumed;
    boolean repeatFired;
    final Runnable onHold = () -> handleLongPress(this);
    final Runnable onRepeat = () -> handleRepeat(this);
}
```

이것을 안전하게 만드는 방벽이 둘이다. 각 타이머는 동작 전에 `touches.get(touch.pointerId) != touch`
를 확인하므로 손가락보다 오래 산 콜백은 아무 일도 하지 않는다. 그리고 `grid` — 페이지, 자판 id, 어느
칸이 어디인지를 바꾸는 모든 것 — 를 색인을 쓰기 전에 대조하므로, 발밑에서 페이지가 바뀐 손가락은 지금
`row, key` 자리에 있는 아무 키나 치는 대신 아무것도 치지 않는다.

**손가락은 확실히 떠날 때까지 자기 키를 유지한다.** 손끝은 결코 가만있지 않고, 누르면서 구른다. 이동마다
키를 다시 정하면 경계에 놓인 탭이 이쪽저쪽으로 뒤집히고, 뗀 자리가 다르다고 누름을 버리면 입력 자체가
사라진다. 이력 현상을 쓸 것: 손가락이 자기 칸에서 **터치 슬롭만큼 온전히 벗어났을 때만** 키가 바뀐다.

```java
/** (x, y) 가 칸 [left, right) × [top, bottom) 에서 slop 만큼 벗어났는가. */
public static boolean escaped(
        float x, float y, int left, int top, int right, int bottom, int slop) {
    return x < left - slop || x > right + slop || y < top - slop || y > bottom + slop;
}
```

```java
if (!escapedKey(layout, touch, x, y)) {
    continue;                                   // 같은 탭이 흔들리는 것뿐이다
}
removeCallbacks(touch.onHold);
removeCallbacks(touch.onRepeat);
touch.row = rowIndex;                           // 진짜로 다른 데로 갔다
touch.key = keyIndex;
armTimers(touch, layout.rows().get(rowIndex).get(keyIndex));
```

**언제 발사되는가: 뗄 때, 그 손가락이 올라가 있는 키로.** 뗄 때 다시 판정하지 말 것 — 키는 이미 이동이
정했고, 두 번째 판정은 반대할 두 번째 기회일 뿐이다. 그 손가락에 대해 홀드나 반복이 이미 처리했다면
탭은 건너뛴다.

```java
if (touch.holdConsumed || touch.repeatFired) {
    return;
}
SoftwareKeySpec held = layout().rows().get(touch.row).get(touch.key);
```

키 자동 반복은 눌린 키를 다시 발사하고 스스로를 다시 예약하는 `postDelayed` 루프다. 누름이 끝나는 모든
경로 — 뗌, 취소, 대상 변경, 그리고 모든 레이어 초기화 — 에서 취소할 것.

**제스처가 무엇을 할지 보여 주기.** 드래그는 일어나기 전까지 보이지 않으므로, 드래그를 받는 키는 눌린
손가락 아래에 그것을 말해 준다. 키를 둘러싼 네 글자와 가운데에 그 키가 쥔 것을 안내판으로 띄우고, 손가락이
간 방향에 불을 켠다. 꾹 누르면 **기다린다** — 뗄 때 불이 켜진 칸이 입력된다 — 반면 곧바로 하는 드래그는
즉시 입력하고 같은 안내판을 이미 정해진 선택과 함께 띄운다. 제스처가 그저 수행되는 것이 아니라 설명된다.
안내판은 캐시된 자판 위에 `onDraw` 에서 그리고, 가장자리 키에서도 밀려나지 않도록 뷰 안으로 물린다.

```java
float centreX = Math.min(Math.max((cellLeft + cellRight) * 0.5f, step + box * 0.5f),
    width - step - box * 0.5f);
```

안내판이 약속하는 것과 제스처가 내놓는 것을 한 곳에 둘 것. 여기서는 정적 메서드
`flickLabel(key, direction)` 이 안내판에 답하고 자동자가 누름에 답하는데, 모든 키와 모든 방향을 훑어 둘이
일치함을 단언하는 테스트가 있다. 그러지 않으면 그림이 코드에서 미끄러지고, 자판이 사용자에게 거짓말을
하기 시작한다.

**검증할 수 있는 것과 없는 것.** 기하와 이력 규칙을 안드로이드 없는 클래스에 두면 평범한 JVM 테스트가
된다. 모든 자판을 몇 픽셀 간격으로 훑어 어느 점에서나 키가 나오는지 보고, 슬롭 규칙을 경계 안·위·밖에서
직접 단언한다. 어떤 테스트도 닿지 못하는 것은 정작 버그를 만든 그것이다 — `adb shell input` 은 포인터를
하나씩만 주입하므로 진짜 두 손가락 겹침은 셸에서 재현할 수 없다. 포인터별 상태는 읽어서 옳음이 드러날
만큼 작게 유지하고, 겹침은 기기에서 손으로 확인할 것.


## 12. 테마

색은 하드코딩하지 말고 기기 테마에서 해석할 것.

```java
static KeyboardPalette resolve(Context context) {
    boolean night = (context.getResources().getConfiguration().uiMode
        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {                                    // Material You: 사용자 팔레트에 맞춘다
            return night
                ? new KeyboardPalette(
                    color(context, android.R.color.system_neutral1_900),   // 배경
                    color(context, android.R.color.system_neutral1_700),   // 키 면
                    color(context, android.R.color.system_accent1_300),    // 활성 키
                    color(context, android.R.color.system_neutral1_50))    // 라벨
                : new KeyboardPalette(
                    color(context, android.R.color.system_neutral1_100),
                    color(context, android.R.color.system_neutral1_50),
                    color(context, android.R.color.system_accent1_600),
                    color(context, android.R.color.system_neutral1_900));
        } catch (RuntimeException useStatic) {
            // 폴백으로 넘어간다
        }
    }
    return night ? DARK : LIGHT;                 // 구버전용으로 다듬어 둔 폴백
}
```

규칙:

- `UI_MODE_NIGHT_MASK`로 라이트/다크를 존중할 것.
- 색조를 고르지 말고 Material 역할로 배정할 것 — 배경은 surface, 키는 올라온 surface, 라벨은
  on-surface, 활성 키는 primary, 눌림은 primary 스테이트 레이어.
- 그리기 캐시 키에 테마를 포함시켜([§11](#11-커스텀-키보드-그리기)) 라이트/다크 전환 시 키보드가 다시
  그려지게 할 것.
- 그리는 모든 표면에 하나의 팔레트를 쓸 것: 키, 롱프레스 팝업, 후보 막대.

### 사용자가 시스템을 덮어쓰게 하기

시스템을 따르는 것은 옳은 기본값이지만 답의 전부는 아니다. 기기를 라이트로 쓰면서 키보드만 어둡게 쓰고
싶은 사용자가 있고, 그가 그렇게 요청할 곳은 여기밖에 없다. 덮어쓰기는 저장된 낱말 하나 — `system`,
`light`, `dark` — 이며 읽는 곳은 정확히 두 군데다.

```java
enum ThemeMode { SYSTEM, LIGHT, DARK;
    boolean night(boolean systemNight) { ... }   // SYSTEM 만 기기 설정을 묻는다
}

static boolean isNight(Context context) {        // 팔레트가 던지는 유일한 질문
    return ScreenTheme.mode(context).night(systemNight(context));
}
```

만들면서 나온 규칙:

- **서수가 아니라 낱말로 저장할 것.** 서수는 열거형 중간에 모드가 하나 끼어드는 날 모든 사용자의 저장된
  선택을 조용히 바꿔 버린다. 알 수 없는 낱말은 `SYSTEM`으로 되돌리면 되고, 그것이 이 설정이 없던 시절의
  동작 그대로다.
- **키보드에는 새 배관이 필요 없다.** 이미 설정 변경을 다시 읽고, 이미 그리기 캐시 키에 테마가 들어
  있다. 그 키에 모드까지 넣으면 전환은 스스로 다시 그려진다.
- **까다로운 쪽은 자기 액티비티다.** 기본 위젯으로 만들어진 화면은 팔레트가 아니라 액티비티 테마에서
  색을 가져온다. `createConfigurationContext`는 API 17, `DayNight`는 API 29이므로 더 낮은 곳까지
  내려가는 앱이라면 테마 리소스를 고르는 편이 낫다 — `SYSTEM`이면 데이·나이트 테마, 아니면 고정된
  라이트/다크 부모를 `onCreate`에서 `super.onCreate` **앞에** 지정하고, 선택이 바뀌면 `recreate()` 한다.
- **잠시 멈춰 있던 액티비티는 옛 테마를 그대로 쓴다.** 사용자가 떠나온 화면은 아직 살아 있으므로,
  `onResume`에서 만들어질 때의 모드와 비교해 달라졌으면 다시 만들 것.


## 12a. 키보드가 사는 창

IME 창은 `Dialog`(`SoftInputWindow`)이며, 액티비티와 달리 시스템 장식만큼 알아서 여백이 잡히지
않는다. 대부분의 ROM은 키보드가 열려 있는 동안 화면 아래에 버튼 두 개 — 키보드 숨김, 키보드 전환 —
를 그리고, 최근 SDK를 타깃하는 앱은 화면 끝까지 그려지므로 창이 화면 물리적 바닥까지 닿아 그 버튼들이
맨 아랫줄 키 위에 올라앉는다.

**이것은 보기 문제가 아니다.** 그 자리의 터치는 시스템이 먼저 가져가므로 아래 깔린 키는 아예 눌리지
않는다. ReteKey도 사용자 신고 전까지 그렇게 나갔다. One UI에서 `!#`와 자판 키를 누를 수 없었다.

```java
// 시스템이 탭을 가져가는 띠만큼 키 아래에 여백을 남긴다.
int band = Build.VERSION.SDK_INT >= 29
    ? insets.getTappableElementInsets().bottom     // 제스처 내비게이션에서는 0
    : insets.getSystemWindowInsetBottom();         // API 20-28
setPadding(0, 0, 0, band);
```

규칙:

- 내비게이션 바가 아니라 **tappable-element** 인셋을 남길 것. 제스처 내비게이션에서는 바가 손잡이
  높이를 보고하지만 그 자리에서 탭을 가져가는 것은 없다. 바 기준으로 여백을 주면 아무 이유 없이
  키보드 한 줄을 버리는 셈이다.
- 띠는 키에서 **덜어내지 말고 아래에 더할 것.** 아니면 버튼을 켠 사용자의 키보드가 말없이 작아진다.
- 변경을 듣는 것과 별개로 붙을 때 한 번 물어볼 것. 인셋은 입력 뷰가 생기기 전에 정해졌을 수 있고,
  리스너는 바뀔 때만 울린다.
- 인셋을 소비하지 말 것. 같은 창의 다른 뷰도 같은 답을 받을 권리가 있다.
- 적용값을 상한으로 묶을 것. 크기 조정 중에 잘못된 인셋이 오면 키가 하나도 남지 않는다.
- API 14~19까지 도는 앱이라면 `WindowInsets` 참조를 한 클래스에만 둘 것. 그 아래에서는 타입 자체가
  없다.

**키보드가 가진 패널에게 화면 전체를 쓸 권리는 없다.** IME 창은 화면 아래에 붙어 있으므로 화면 높이
그대로 잰 창은 위쪽 상태 표시줄 뒤까지 올라간다. 그리고 자기 영역이 그 창에 눌려 없어진 앱은 입력기가
방해가 된다고 판단해 내려 달라고 할 수 있다. 그것이 열리자마자 닫히는 패널이다(§15.52). 패널은 입력기가
실제로 가진 공간에 대고 재라. 화면에서 위쪽 띠를 빼고, 키보드가 아래에 이미 비워 둔 만큼을 빼고, 계산이
무엇이라 하든 화면의 5분의 4 를 넘지 않게. 이것은 산수이므로 레이아웃 과정이 아니라 단위 시험이 부를 수
있는 클래스에 있어야 한다.

**떠 있는 패널은 화면 크기의 창 안에 산다.** 그 결과가 둘이다.

- `onComputeInsets` 에서 프레임워크에 넘기는 터치 영역은 **창** 좌표다. 패널의 경계는 패널 자신의
  좌표이므로 `getLocationInWindow` 만큼 옮길 것. 프레임워크가 입력 뷰 위에 무엇을 더하면 —
  candidates 띠 — 패널이 그만큼 내려가고, 그러지 않으면 영역이 키보다 그만큼 위의 터치에 답한다.
- **제목 줄 전체가 이동 손잡이다.** 거의 비어 있는 막대의 한쪽 끝에 있는 작은 손잡이는 놓치기 쉬운
  표적이다. 막대에서 키(건너가기·닫기·크기 조절)가 아닌 곳은 어디를 끌어도 이동한다(0.1.171).

## 13. 설정과 영속화

설정 화면은 `method.xml`에 이름을 적는 평범한 `Activity`다. 구조적으로 유일한 요점은 **서비스와 액티비티가
서로 다른 시점의 상태를 공유한다**는 것이고, 키보드가 변경을 알아채야 한다는 것이다.

```java
// 키보드 뷰에서: attach 때 읽고, 이후 변경도 따라간다.
private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
    (prefs, key) -> reloadPreferences();

@Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    prefs().registerOnSharedPreferenceChangeListener(prefsListener);
    reloadPreferences();
}

@Override protected void onDetachedFromWindow() {
    prefs().unregisterOnSharedPreferenceChangeListener(prefsListener);
    super.onDetachedFromWindow();
}
```

리스너가 없으면 키보드가 살아 있는 동안 바꾼 설정은 뷰가 다시 만들어질 때에야 반영되고, 사용자는 그것을
"슬라이더가 아무 일도 안 한다"로 읽는다.

저장 값의 클램프는 순수 자바에 두어 단위 테스트할 수 있게 하고, 쓸 때만이 아니라 **읽을 때도** 클램프할
것. 프리퍼런스 파일은 내 검증 규칙보다 오래 산다.

설정에 들어가는 것 중 이름을 붙여 둘 만한 갈래가 둘 있다. 둘 다 쓰는 쪽(설정 화면)이 아니라 읽는
쪽(키보드)이 읽는다.

- **그리기 규칙이 필요로 하는 값.** 방금 친 글자를 보여 주는 상자는 스위치 하나와 불투명도 하나다.
  불투명도에는 글자 쪽 바닥값이 있다. 배경과 함께 글자까지 흐려지는 상자는 안 보이게 되기 전에 이미
  읽을거리가 아니게 되기 때문이다(§15.50, §15.51).
- **사용자가 가져온 것.** 직접 쓴 레이아웃은 파일의 글 그대로 저장하고, 들어올 때 한 번 그리고 서비스가
  시작할 때 다시 파싱하며, 자리는 하나다. 새것이 오면 앞의 것을 대신한다. 파싱한 결과가 아니라 글을
  저장하는 덕분에, 나중 판이 옛 파일을 저장 당시의 판보다 더 잘 읽을 수 있다. 형식과 "일부러 말하지 않는
  것"은 별도 문서다: `docs/user-layouts.ko.md`(§15.47).

## 14. 테스트와 검증

**입력 코어를 안드로이드 비의존으로 유지할 것.** 이벤트 정규화, 레이아웃 기하, 조합 오토마타, 사전,
설정값 클램프는 모두 순수 자바로 만들 수 있고, 그러면 기기 없이 JVM에서 단위 테스트할 수 있다. 실제
로직 버그는 거의 다 여기서 잡힌다.

픽스처만이 아니라 **배포되는 데이터**도 테스트할 것. 실제 사전 파일을 단위 테스트에서 파싱해 알려진
변환 몇 개를 검증하면, 잘못된 재생성이 빌드를 깨뜨린다.

```java
@Test public void shippedDataConvertsCommonReadingsAndWords() {
    HanjaTable table = HanjaTable.parse(Files.readAllLines(
        Paths.get("src/main/assets/hanja.txt"), StandardCharsets.UTF_8));
    assertTrue(table.candidates("가").contains("家"));
    assertTrue(table.candidates("학교").contains("學校"));   // 단어 항목, 최장 일치
}
```

**헤드리스 에뮬레이터를 IME 동작의 판정 기준으로 믿지 말 것.** KVM 없는 헤드리스 에뮬레이터에서 반복
실측된 두 가지 실패 양상:

- IME 창이 `Requested w=0 h=0`, `mViewVisibility=GONE`, `mHasSurface=false`로 보고되고 `screencap`은
  빈 프레임버퍼를 돌려준다 — *실기기에서 정상 동작이 확인된 빌드에서도 똑같이*. 이것으로 "키보드가 안
  뜬다"고 결론지을 수 없다.
- 주입한 키 이벤트(`adb shell input keyevent`)가 IME의 `onKeyDown`에 아예 도달하지 않을 수 있고,
  실행할 때마다 결과가 달라지기까지 한다.

헤드리스 에뮬레이터가 *증명할 수 있는 것*: APK 설치, IME 등록·선택 가능, 액티비티 인플레이트, 그리고
크래시가 없다는 것. 쓸모 있는 명령:

```sh
adb shell ime list -a
adb shell settings get secure default_input_method
adb shell dumpsys input_method | grep -E "mInputShown|mServedView|mCurMethodId"
adb logcat -d | grep -E "FATAL|AndroidRuntime"
```

시각적이거나 입력 상호작용이 필요한 것은 반드시 실기기에서 확인해야 한다. 회귀가 의심될 때는 **동작이
확인된 태그**를 빌드해 같은 에뮬레이터에 설치해 볼 것. 그 빌드에서도 증상이 같다면, 그 증상은 내 변경이
아니라 환경이다.

**시험용(instrumentation) 빌드는 에뮬레이터를 계측 도구로 바꾼다.** 키보드를 보여 줄 수는 없지만,
누르고 무슨 일이 일어났는지 읽어 올 수는 있다. ReteKey 는 git 에서 제외한 `instrumentation` 소스
세트에 서비스를 상속한 클래스를 두고 브로드캐스트 수신기를 등록한다. 아래 명령은 모두 이 매뉴얼이
이제 답하는 질문 하나씩을 풀려고 더한 것이다.

- **진짜 소프트 경로로 친다.** 터치가 만들었을 이벤트를 `dispatchSoftwareInput` 에
  넘긴다(`keys=cho0,jung0,RAW:C:CTRL`). 키 사이에 간격을 두어 편집기 보고가 엄지로 칠 때처럼
  끼어들게 한다. 결과는 편집기에서 읽는다 — Termux 에서 셸이 쓴 파일(`run-as com.termux cat`), 또는
  리플렉션으로 읽은 칸의 글자와 선택 영역.
- **그릴 표면 없이 IME 창을 찍는다.** `decor.draw(new Canvas(bitmap))` 는 창의 뷰를 비트맵에 그려
  꺼내 볼 수 있게 한다. 뷰 트리 덤프(클래스·가시성·크기·위치)가 그림이 무엇인지 설명한다. 보이지
  않던 띠와 떠 있는 패널의 배치를 이렇게 봤다(§10).
- **IME 자신의 뷰를 터치한다.** 액션바 칸이나 떠 있는 틀에 `MotionEvent` 를 보내면 진짜 터치 코드 —
  탭·길게 누르기·반복·끌기 — 가 돈다. `adb input` 은 거기에 닿지 않는다.
- **물리 키를 누른 채로 둔다.** 수정자로 `onKeyDown`/`onKeyUp` 을 부르고 명령 사이에 눌린 채로 둔다.
  `adb input keyevent` 는 TCG 에서 시간 초과로 끝나고 무엇도 누르고 있을 수 없다.
- **클립보드를 읽고 쓴다.** 안드로이드는 현재 IME 의 프로세스에 그것을 허락한다.

원격 데스크톱이면 저쪽 끝도 잰다. 에뮬레이터에 Microsoft 클라이언트를 설치해 Windows VM 에 붙이고,
로그인된 세션 안의 예약 작업으로 세션 클립보드를 읽고 쓰며, 게스트 안에서 화면을 찍는다(§15a.7,
§15a.8). 그래도 에뮬레이터가 알려 줄 수 없는 것은 진짜 손가락과 진짜 화면이 어떻게 느끼느냐 — 크기,
감촉, 띠를 읽을 수 있는지 — 이므로 그것은 실기기 점검표에 남는다.

**바꾼 칸만이 아니라 조합표의 모든 칸을 친다.** 이 키보드가 내보낸 결함 대부분은 아무도 쳐 보지 않은
조합에 있었다. 터미널에서 다시 그리는 음절, 음절 도중에 그대로 넘기는 키, 음절 직후에 누른 방향키.
이제 조합표 둘이 그것들을 함께 돌린다.

- `InteractionMatrixTest`(JVM, 빌드마다): 편집기 다섯 종류 — 일반 입력칸, 원격 데스크톱, 띠 방식
  터미널, 직접 그리는 터미널, Termux 텍스트 모드 — × 입력 방식 열네 가지. 각 장면은 모든 편집기가
  똑같이 보여야 할 화면 하나로 끝난다. 키는 진짜 디스패처·조합기·실행기를 지나고, 편집기는
  `SimulatedEditors` 의 모델이며 각 규칙은 흉내 낸 동작의 출처를 적는다. JVM 이 돌릴 수 없는 서비스
  부분(키 넘기기, 칸 떠나기, 선택 보고에 반응하기)은 흉내 낸 메서드를 가리키며 재현한다. 표 전체를
  찍고 실패한 칸을 모두 이름으로 댄다.
- `scripts/interaction-matrix.sh`(에뮬레이터, 시험용 빌드, TCG 에서 약 20분): 진짜 편집기와 진짜 IME
  창만 답할 수 있는 칸 — 두 모드의 진짜 Termux, 띠의 뷰 트리, 터미널 안에서 열려 그대로 있는 노트와 클립
  목록, 이름으로 아는 터미널에 붙여넣기, 물리 수정자를 누른 채 액션바 터치, 음성기호 페이지, 파일에서
  레이아웃 설치, 키패드. PASS/FAIL 표를 찍고 실패한 칸의 수로 끝난다(v0.1.192 기준 예순여섯 칸).

**레인은 데이터를 지우고 부팅하므로 터미널을 매번 다시 깔아야 한다.** `emulator-lane.sh` 는
`-wipe-data` 로 에뮬레이터를 띄운다. 한 번의 실행이 지난 실행에 기대지 않게 하는 장치지만, 그 때문에
Termux 도 부팅마다 사라진다. 조합표가 찾을 수 있는 자리에 APK 를 두어야(고정 경로를 보고 패키지가
없으면 설치한다) 하고, 아니면 터미널 절반이 통째로 건너뛰어진다 — 표만 보면 조용히. Termux 의 릴리스
빌드는 **디버그 가능이 아니므로** `run-as com.termux` 로는 칸이 쓴 파일을 읽지 못한다. 레인의 AOSP
이미지는 `adb root` 를 주고 그것으로는 읽히므로, 스크립트는 run-as 를 먼저 쓰고 안 되면 데이터
디렉터리를 직접 읽는다. IME 창이 화면에 떠 있는 동안 APK 를 다시 설치하면 프레임워크가 `showWindow`
에서 `View=DecorView[InputMethod] not attached to window manager` 를 던진다. 이것은 재설치가 만든
허상이지 결함이 아니다. 그렇게 본 죽음은 IME 를 강제 종료하고 앱을 다시 띄운 뒤에 믿어라.

**남이 파일을 쓰게 하는 문서도 시험하라.** 형식은 사람들이 베끼는 설명만큼만 좋고, 설명은 조용히 어긋난다.
`docs/user-layouts.md` 의 예시는 단위 시험이 뽑아내 파싱하고, 번역본은 같은 예시를 바이트 단위로 그대로
담아야 하며, 그중 첫 번째는 설정 화면이 보여 주는 바로 그 문자열이다. 그래서 전화기의 화면, 웹의 문서,
파서 셋이 서로 다른 말을 할 수 없다(§15.47).

**조합표가 무언가를 잡는다는 것을 증명하라.** 결함 때문에 칸을 더했으면 고치기 전 코드에 한 번 돌려
실패하는 것을 볼 것. 통과만 해 본 칸은 자기 모델을 시험하고 있을지 모른다. 그리고 칸이 실패하면 믿기
전에 진짜 편집기에서 재현할 것. JVM 조합표의 첫 실행은 열두 칸이 실패했는데, 엔터 줄은 모델 탓(여러
줄 입력칸은 엔터 동작을 요청하지 않는다)이었고 나머지는 진짜 결함이었다(§15.34).

## 15. 안티패턴과 그것을 가르쳐 준 실패들

### 15.1 빠져나올 수 없는 엄격한 기대 원장

**만든 것.** 모든 편집이 원장(ledger)에 "기대"를 예약하고 `onUpdateSelection`이 그것을 확인해야 했다.
모순이 생기면 `desynchronize()`가 세션을 `DESYNCHRONIZED` 상태로 옮겼고 — 그 상태에서는 이후 모든 키가
거부됐다.

**벌어진 일.** 터미널에서는 `onUpdateSelection`이 미상 값을 보고하는데 그것이 모순으로 집계된다. 결국
몇 글자 치다가 첫 미상 갱신을 만나면 래치가 걸리고 그 세션 내내 키보드가 죽는다. 이 버그는
*"몇 글자 치다가 멈춘다"*로 세 번 보고됐고, 문을 하나씩 막는 식으로 세 번 패치됐다.

**해결.** 그 계층을 삭제. 확인도, 예약도, 실패 상태도 없는 수동적 커서 캐시로 대체. 내부 장부 때문에
입력이 거부되는 일이 원천적으로 불가능해진다.

```java
// 틀림: 장부가 입력을 거부할 수 있다.
if (state == DESYNCHRONIZED) {
    return ExecutionResult.notDispatched(Reason.SESSION_DESYNCHRONIZED);
}

// 옳음: 그런 상태가 없다. 미상은 그냥 미상이다.
cursor = reported.isKnown() ? reported : EditorBounds.unknown();
```

**규칙.** "내 모델이 불확실해서 타이핑을 거부한다"고 답할 수 있는 코드 경로가 있다면 그 자체가 버그다.
그 답을 가능하게 하는 상태를 지울 것.

### 15.2 삽입을 "선택영역을 안다"로 막은 것

**만든 것.** 캐시된 bounds가 미상이면 `commitText`와 `setComposingText`를 `INVALID_SELECTION`으로
거부했다.

**벌어진 일.** 터미널은 `-1`을 보고하므로 모든 키가 드롭됐고 매번 실패 토스트가 떴다 — 그 앱에서는
키보드가 완전히 고장 난 것처럼 보였다.

**해결.** 삽입은 커서 기준이라 bounds가 필요 없다. `deleteSurroundingText`도 마찬가지다. 백스페이스
역시 같은 조건으로 막혀 있었고, 그 게이트를 완전히 제거하고서야 터미널에서 동작했다.

```java
// 틀림.
if (!bounds.hasSelection()) {
    return refuse(Reason.INVALID_SELECTION);
}
ic.commitText(text, 1);

// 옳음: 에디터는 자기 커서가 어디 있는지 안다.
ic.commitText(text, 1);
```

**규칙.** *절대* 위치를 다루는 연산만 선택영역을 요구할 수 있다 — 그리고 실무적으로 그런 연산은 없어야
한다.

### 15.3 원시 엔터를 거부한 것

**만든 것.** 엔터는 에디터 액션으로 해석됐고, 액션이 없는 한 줄짜리 에디터에서는 빈 결과가 되어 아무
일도 하지 않았다. 게다가 "리치" 에디터에 대해서는 원시 엔터를 추가로 거부했다.

**벌어진 일.** 터미널에서 엔터가 전혀 동작하지 않았다 — 줄바꿈도, 전송도 없었다.

**해결.** 최종 폴백으로 `sendKeyEvent`를 통해 실제 `KEYCODE_ENTER`를 보낸다. 실제 엔터 키는 모든
에디터가 이해한다. 터미널은 줄을 전송하고, 평범한 필드는 기본 동작을 한다. 엔터 처리 순서는
[§7](#7-에디터-종류와-미상-선택영역) 참고.

**규칙.** "의미"로 매핑되는 키에는 원시 키 폴백을 남겨 둘 것. 사용자가 누른 키에 대해 아무 일도 하지
않는 것은 결코 정답이 아니다.

### 15.4 예측 불일치마다 조합을 재앵커한 것

**만든 것.** `onUpdateSelection`에서 보고된 커서가 예측과 다르면 조합기가 `finishComposingText()`를
호출하고 리셋했다. 사용자가 다른 곳을 탭했을 때 재앵커하려는 의도였다.

**벌어진 일.** 실제 에디터는 예측과 정확히 일치하지 않는다. 불일치는 IME *자신의* 편집에서도 터졌고,
그래서 키를 칠 때마다 finish+reset이 발생해 `onUpdateSelection`을 재진입시킨 것으로 보이며, IME가
죽었다. **키보드가 아예 뜨지 않게 됐다.** 이 상태로 배포됐고 되돌려야 했다.

```java
// 틀림: 예측 불일치는 아무것도 증명하지 않는다.
if (!reported.equals(predicted)) {
    ic.finishComposingText();
    composer.reset();
}

// 옳음: 명확한 신호일 때만 재앵커한다 — 커서가 조합 영역을 벗어났을 때.
if (composer.isComposing() && !composingRegion.contains(reported.start())) {
    ic.finishComposingText();
    composer.reset();
}
```

**규칙.** 예측 불일치로 파괴적 상태 변경을 일으키지 말 것. 예측은 힌트다. 사용자에게 보이는 상태를
초기화하는 것은 명백한 증거일 때뿐이어야 한다.

### 15.5 예외를 밖으로 흘린 것

**벌어진 일.** 키나 선택 갱신을 처리하다 던져진 예외가 IME 프로세스를 죽였고, 타이핑 도중 키보드가
사라졌다 — 문제를 일으킨 앱만이 아니라 모든 앱에서.

**해결.** 에디터를 마주하는 진입점을 감싸고, 전파 대신 강등할 것:

```java
try {
    execute(dispatcher.dispatch(event));
} catch (RuntimeException crash) {
    dispatcher.reset();
    inputProcessor.reset();   // 반쯤 만들어진 음절은 잃되, 키보드는 살린다
}
```

**규칙.** 잘못 동작하는 에디터 하나가 키보드를 무너뜨릴 수 있어선 안 된다. 음절 하나를 잃는 것은
받아들일 수 있지만 키보드를 잃는 것은 안 된다.

### 15.6 애플리케이션 단축키를 삼킨 것

**벌어진 일.** IME가 수정자 조합을 소비해서, 키보드가 활성인 동안 어느 앱에서도 Ctrl+A / Ctrl+C /
Ctrl+V가 동작하지 않았다.

**해결.** 수정자 키와 Ctrl/Alt/Meta 조합은 `super.onKeyDown`으로 위임할 것 — 단, 사용자가 IME 기능에
바인딩한 특정 조합은 그보다 먼저 검사해야 한다([§9](#9-물리-키보드)).

### 15.7 소프트 수정자를 편집 명령에만 매핑한 것

**만든 것.** 소프트 Ctrl + 글자를 컨텍스트 메뉴 동작으로 매핑했는데, `a/c/v/x/z/y`에 대해서만이었다.
나머지 글자는 그대로 텍스트로 입력됐다.

**벌어진 일.** 터미널에서 Ctrl+B가 `0x02`를 보내는 대신 글자 `b`를 입력했다.

**해결.** *모든* 글자에 대해 실제 키 조합을 보낼 것. 리치 에디터는 `onKeyShortcut`으로
Ctrl+A/C/V/X/Z/Y를 처리하고 터미널은 제어 코드를 받는다 — 특수 케이스 표 대신 하나의 메커니즘이다.

```java
// 틀림: 내가 떠올린 것만 담긴 효과 표.
int id = contextMenuIdFor(letter);      // a→selectAll, c→copy, … 없으면 0
if (id != 0) ic.performContextMenuAction(id); else ic.commitText(letter, 1);

// 옳음: 실제 이벤트를 내보내고 해석은 각 에디터에 맡긴다.
sendRawKey(keyCodeForLetter(letter), KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
```

**규칙.** 효과를 흉내 내기보다 실제 입력 이벤트를 내보내는 쪽을 택할 것. 이벤트는 내가 만든 효과 표보다
더 많은 곳에서 통한다.

### 15.8 키를 칠 때마다 키보드 이미지를 통째로 다시 만든 것

**비용.** 키 하나 바뀌는 변화를 위해 30–40개 키의 그라디언트·그림자·텍스트를 매번 UI 스레드에서 다시
계산하게 된다.

**해결.** [§11](#11-커스텀-키보드-그리기) — 정적 이미지를 캐시하고 키 하나만 다시 그린다. 눌린 키를 캐시
키에 넣으면 아무것도 캐시하지 않은 것이 된다.

### 15.9 색을 하드코딩한 것

**벌어진 일.** 고정 팔레트는 시스템이 다크 모드로 바뀌는 순간 어색해졌고, 사용자 테마를 전혀 반영하지
못했다.

**해결.** [§12](#12-테마) — 테마에서 해석하고, 캐시 키에 테마를 포함시킬 것.

### 15.10 에뮬레이터를 믿은 것

**벌어진 일.** 헤드리스 에뮬레이터가 크기 0에 한 번도 렌더되지 않은 IME 창을 보여 주고 주입한 키
이벤트를 흘렸는데, 이를 코드 회귀로 오인했다. 동작이 확인된 태그를 빌드해 같은 에뮬레이터에 설치하자
동일한 증상이 재현되어, 원인이 환경임이 드러났다.

**규칙.** "키보드가 고장 났다"고 결론짓기 전에, 동작이 확인된 빌드로 재현해 볼 것.
[§14](#14-테스트와-검증) 참고.

### 15.11 첫 포인터와 마지막 포인터만 들은 것

**벌어진 일.** `onTouchEvent` 가 `ACTION_DOWN` 과 `ACTION_UP` 만 처리했다. 빠르게 치면 손가락이
겹친다 — 앞 손가락이 떨어지기 전에 다음 손가락이 내려앉는다 — 그 누름은 `ACTION_POINTER_DOWN` 으로
오는데 `default` 로 흘러 무시됐다. 다른 손가락이 아직 닿아 있는 동안 누른 키는 전부 조용히 사라졌고,
더 나쁘게는 마지막 `ACTION_UP` 이 **첫** 손가락의 키를 놓아 주어서, ㅈ 를 누른 채로 친 ㅅ 가 ㅈ 로
나왔다. 사용자에게는 키보드가 입력을 삼키는 것으로 보이고, 탭을 하나씩 주입하는 테스트로는 보이지
않는다.

**해결.** 포인터마다 자기 키를 갖게 하고, 타이머도 각자 갖게 한다.

```java
case MotionEvent.ACTION_DOWN:
case MotionEvent.ACTION_POINTER_DOWN:
    beginTouch(event.getPointerId(index), event.getX(index), event.getY(index));
    return true;
case MotionEvent.ACTION_UP:
case MotionEvent.ACTION_POINTER_UP:
    endTouch(event.getPointerId(index), event.getX(index), event.getY(index));
    return true;
```

```java
private final SparseArray<Touch> touches = new SparseArray<>();

private final class Touch {
    final int pointerId; final int row; final int key;
    boolean holdConsumed; boolean repeatFired;
    // 두 엄지로 치면 이것이 동시에 둘이므로, 길게 누르기와 자동반복 타이머를 뷰의 필드로 둘 수 없다.
    final Runnable onHold = () -> handleLongPress(this);
    final Runnable onRepeat = () -> handleRepeat(this);
}
```

각 타이머는 동작하기 전에 `touches.get(touch.pointerId) != touch` 를 확인한다. 손가락이 떨어진 뒤에
뒤늦게 깨어난 콜백이 아무 일도 하지 않게 하기 위해서다. 눌림 강조도 잡고 있는 키를 전부 그린다.

**규칙.** 키보드가 강조를 하나만 그릴 수는 있어도, 포인터는 전부 **들어야** 한다. 뷰가 누름마다 들고
있는 것 — 잡은 키, 길게 누르기 타이머, 반복 타이머, "홀드가 이미 처리했다" 표시 — 은 뷰가 아니라
손가락의 것이다. 그리고 이 방식으로 검증할 수 없는 것을 알아 둘 것: adb 는 포인터를 하나씩만 주입하므로
버그를 만드는 겹침 자체를 셸에서 재현할 수 없다. 포인터별 상태는 읽어서 옳음이 드러날 만큼 작게 유지할 것.

### 15.12 반복 이벤트의 의미를 버린 것

**벌어진 일.** 블루투스 키보드에서 키를 누르고 있으면 한 글자만 나오고 조용해졌다. 플랫폼은 눌린 키를
`getRepeatCount()` 가 올라가는 `ACTION_DOWN` 으로 계속 보내는데, 두 층이 각자 그것을 버리고 있었다.
이벤트 정규화기는 `repeatCount != 0` 인 것에 의미를 붙이기를 거부했고, 디스패처는 추적 중인 반복에
"처리됨, 동작 없음" 으로 답했다 — 삼킨 것이라 에디터도 보지 못했다.

**규칙.** 반복도 키 입력이다. 첫 누름을 소비했다면 반복도 소비하고 **처리**해야 한다. 아니면 둘 다
위임해야 한다. 각각 반쪽씩이 바로 입력을 잃는 유일한 조합이다.

### 15.13 백스페이스가 방금 넣은 것을 지운다고 가정한 것

**벌어진 일.** 12키 방식은 화면의 자모를 바꿀 때 백스페이스를 보내고 다음 자모를 보낸다 — "그 탭이
ㅗ 를 ㅜ 로 바꿨다"를 표현하는 자연스러운 방법이다. 겹모음 전까지는 잘 됐다. 조합기는 겹중성을
지우는 것이 아니라 **분해**하므로 과 는 ㄱ 이 아니라 고 로 돌아간다. 그래서 대체 자모가 남은 모음 옆에
붙어 ㆍㅡㅣㆍ 가 과 가 아니라 고ㅘ 를 냈다.

**규칙.** 지우고 다시 넣어 조합기를 몰 것이라면 그 조합기의 삭제 의미까지 책임져야 한다. 어떤 자모가
분해되는지 알고 그때는 두 번 지울 것 — 그리고 층과 층 사이가 아니라 **조합기를 지나 에디터 글자까지**
검사할 것. 표 수준 테스트는 그동안 내내 통과하고 있었다.

### 15.14 기기에 글꼴이 없는 기호

**벌어진 일.** 메뉴 키를 `☰` 로 그렸다. 안드로이드 4.4 에서 캔버스는 **아무것도** 그리지 않았다.
그 기호를 덮는 글꼴이 플랫폼에 없어서 키가 빈 칸이 됐다. 기호 라벨 대부분이 같은 처지였다. 어떤
테스트도 이것을 볼 수 없었다 — 스크린샷을 찍어서 발견했다.

**규칙.** 구형 플랫폼에서는 렌더되는 것을 직접 보지 못한 기호보다 짧은 낱말이 낫다. 대체는 API 레벨을
키로 하는 표 한 곳에 두어 자판 데이터는 기호를 그대로 두고 그리는 것만 바뀌게 할 것. 그리고 지원하는
가장 낮은 버전을 스크린샷으로 볼 것. 빈 키는 당신이 떠올릴 어떤 단언에도 걸리지 않는다.

### 15.15 디스크에 둬도 될 사전을 파싱한 것

**벌어진 일.** 190 KB 의 한자 표가 해시맵 안에서 `String` 이 되면서 자바 힙 약 2.6 MB 가 됐다. 항목
2만 개 각각에 객체 헤더와 char 배열이 붙는데, 정작 조회는 한 번에 하나만 본다.

**해결.** 표를 정렬해 배포하고, 매핑할 수 있도록 압축하지 않고 저장한 뒤
(`androidResources { noCompress += ["idx"] }`), 매핑한 바이트를 제자리에서 이등분 탐색한다. 데이터는
힙에 들어오지 않는다. 파일 기반 페이지라 메모리가 부족하면 커널이 버리고, 다음 조회가 건드리면 다시
읽는다.

**규칙.** 바뀌는 것보다 읽히는 일이 훨씬 잦은 조회 표는 맵이 아니라 디스크에 있어야 한다. 배포 형식을
생성한다면, 그것이 예전 형식과 **모든 항목에서 양방향으로** 같은 답을 내는지 테스트할 것. 여기서 후보
순서가 어긋난 것을 잡아낸 것이 바로 그 대조였다.

### 15.16 구멍 뚫린 터치 영역

**벌어진 일.** 키는 4dp 간격을 두고 그려지는데, 히트 테스트도 같은 간격만큼 안으로 밀려 있었다.
"경계 근처를 쳐도 옆 키로 잘못 들어가지 않게" 하려고 간격에 떨어진 터치는 아무것도 받지 않았다.
240dpi 기기에서 간격은 6px, 칸은 48×70px 이니 두 키 사이의 죽은 띠가 가로로 48 중 12, 세로로 70 중
12 — **자판 넓이의 약 3분의 1이 아무것도 입력하지 않았다.** 이것은 결코 버그처럼 보이지 않는다.
자판이 둔한 것처럼 보이고, 사용자는 더 세게 누르게 된다.

작은 구멍이 둘 더 있었다. 뗄 때 다시 히트 테스트를 해서 누르기 시작한 키와 다르면 버렸으므로 손끝이
구르는 것만으로 입력이 취소됐고, 이동은 아예 듣지 않았으므로 손가락의 키가 손가락을 따라갈 수 없었다.

**해결.** 모든 픽셀이 어떤 키엔가 속한다 — 간격은 그림에 있는 것이지 목표물에 있는 것이 아니다.
손가락은 시작한 키를 그 칸에서 **터치 슬롭만큼 벗어날 때까지** 유지하고, 그다음에야 옮겨간 키를 갖는다.

```java
public static boolean escaped(
        float x, float y, int left, int top, int right, int bottom, int slop) {
    return x < left - slop || x > right + slop || y < top - slop || y > bottom + slop;
}
```

뗄 때는 그 손가락이 올라가 있는 키가 입력된다. 떼는 위치는 반대할 두 번째 기회가 아니다.

**규칙.** 눈에 보이는 간격은 터치의 간격이 아니다. 잘못된 키를 막겠다고 히트 박스를 깎지 말 것 —
드물게 틀린 글자 하나를 막으려다 흔하게 빠지는 글자를 얻는데, 사람이 느끼는 것은 빠지는 쪽이다.
죽은 영역(아무 답도 않는 넓이) 대신 이력 현상(손가락이 넘어서야 하는 슬롭)을 쓰고, 목표물이 빈틈 없이
표면을 덮게 할 것. 이건 단위 테스트로 그대로 단언할 수 있다: 모든 자판을 몇 픽셀 간격으로 훑어 어느
점에서나 키가 나오는지 본다.

### 15.17 글자가 끝나기도 전에 확정된 음절

**벌어진 일.** 나랏글로 많을 쓸 수 없었다. 12키 자동자는 대부분의 글자를 화면에 있는 것을 바꿔서
얻는다 — ㅇ 다음 획추가가 ㅎ 이다 — 그래서 자판은 그 글자가 무엇이 될지 **알기 전에** 글자를 친다.
획이 도착할 무렵 조합기는 이미 ㅇ 을 보았고, ㄴ + ㅇ 은 겹받침이 아니라고 판단했고, 만 을 에디터에
확정했고, 새 음절을 시작한 뒤다. 그래서 획의 지우고-다시-치기는 만 ㅎ 을 냈다. ㅎ 이 붙을 곳이 없었기
때문이다 — 그것이 속할 음절은 더 이상 조합기가 손댈 수 있는 것이 아니었다.

곧바로 쳐 넣는 겹받침은 처음부터 잘 됐다 — 값, 삶, 닭 — 그래서 표는 결백해 보였다. 못 쓰는 것은 꼬리가
변형을 거쳐야 하는 겹받침 전부였다: 많, 않, 앉, 옳, 핥.

**해결.** 자판이 지우는 두 가지 이유를 구별한다. 사용자의 백스페이스는 *보이는 것을 되돌려라* 라는
뜻이고, 자동자의 것은 *방금 친 글자를 돌려 다오, 그게 실은 무엇이었는지 이제 말하겠다* 라는 뜻이다.
후자에게만 조금 전 닫힌 음절을 다시 여는 것을 허락한다.

```java
public Result reopenClosedSyllable() {
    if (closedSyllable == null || state != State.CHO) {
        return null;
    }
    cho = closedSyllable[0];
    jung = closedSyllable[1];
    jong = closedSyllable[2];
    state = jong > 0 ? State.CHO_JUNG_JONG : State.CHO_JUNG;
    closedSyllable = null;
    return preedit(syllable());
}
```

조합기는 자음이 밀어낸 음절을 **딱 한 입력 동안만** 기억한다. 다른 입력은 그것을 지우므로, 바로 뒤에
오는 변형만 쓸 수 있다. 처리기는 그것을 편집 동작 셋으로 쓴다: 교정 중인 자음을 지우고, 확정된 음절을
에디터에서 도로 빼내고, 그것을 다시 조합 중으로 만든다. 그러면 대체 자음이 평범한 겹받침 규칙으로
합쳐지고, 많 이 스스로 조립된다.

**규칙.** 아직 고칠 수 있는 글자를 치는 자판에게 "확정"은 아직 할 수 없는 약속이다. 확정을 미루든지,
되돌릴 길을 남겨 둘 것 — 그리고 두 종류의 삭제를 같은 값이 아니라 **어휘에서 서로 다른 값**으로 둘 것.
자동자는 사용자의 백스페이스가 모르는 것을 알고 있기 때문이다. 검사는 자모가 아니라 **음절이 있는
자리**에서 할 것: 자모 표는 내내 옳았다.

### 15.18 손가락이 움직이는 동안에만 존재하는 제스처

**벌어진 일.** 키에서 드래그하는 것을 `ACTION_MOVE` 에서만 인식했다. 손가락이 문턱을 넘는 순간 드래그한
글자가 들어갔다. 엄지로는 됐고 `adb shell input swipe` 의 짧은 지속시간에서는 안 됐다 — 주입된 획이
누름과 뗌만으로 오고 그 사이 이동이 없어서, 자판은 그 키의 탭 글자를 쳤다. 시스템이 빠른 획을 합쳐
버리면 진짜 손가락에도 같은 구멍이 있다.

**해결.** 제스처를 두 곳에서 판정한다. 이동 처리기는 여전히 선을 넘는 즉시 발사한다 — 두 번 두드리는
대신 드래그하는 이유가 그것이다 — 그리고 뗄 때 같은 것을 한 번 더 확인한다. 오는 길에 아무것도 보고하지
않은 획을 위해서다.

```java
if (tryFlick(layout(), touch, x, y)) {
    // 오는 길에 이동을 보고하지 못할 만큼 빨랐어도 드래그는 드래그다.
    return;
}
```

`tryFlick` 이 그 누름을 소비 표시하므로 두 경로가 함께 치는 일은 없다.

**규칙.** 중간 이벤트로만 정의된 제스처는 시스템이 버려도 되는 제스처다. 시작점과 끝점으로 판정하고,
중간 이벤트는 그것을 즉각적으로 **느끼게** 하는 최적화로 둘 것 — 정의로 두지 말 것. 그리고 이것을
드러낸 측정을 기억할 것: 에뮬레이터에서 `input tap` 두 번을 이어 부르는 데 **10초**가 걸렸다. 그보다
짧은 시간 제한을 가진 동작은 셸에서 아예 시험할 수 없다. 테스트 수단이 닿지 못하는 동작이 무엇인지 알고,
검증했다고 말하는 대신 그렇다고 말할 것.

### 15.19 두 가지 모양의 화면에 설정 하나

**벌어진 일.** 높이, 고를 수 있는 자판, 플로팅 패널과 그 투명도가 각각 값 하나였다. 세워 든 전화기에서
여유를 남기도록 맞춘 높이는 눕힌 화면을 삼켜 버리고, 넓은 화면에서 제 몫을 하는 플로팅 패널은 긴 화면에서
거치적거린다. 사용자는 제대로 맞춰 놓고 기기를 돌린 뒤 다시 맞춰야 했다.

**해결.** 화면의 모양에 달린 설정에는 값을 둘씩 준다. 키 이름 뒤에 방향을 붙이고, 지금 그리는 방향의
것을 읽는다.

```java
public String key(String base) {
    return base + "." + suffix;          // height_scale.portrait, height_scale.landscape
}
```

이 분리를 안전하게 만드는 것이 둘이다. 읽기는 분리 이전의 접미사 없는 키로 되돌아가므로, 갱신해도 사용자가
고른 값이 **양쪽 방향 모두에서** 유지된다 — 각각 따로 설정되기 전까지는. 그리고 쓰기는 방향 붙은 키에만
가므로, 한쪽을 바꾸는 순간 그쪽은 옛 공용 값을 따르기를 멈춘다. 설정 화면은 기기를 따라가지 않고 **지금
어느 방향을 설정하는지**를 스스로 말한다. 설정을 바꾸려고 전화기를 돌리면 정작 보러 온 설정이 시야에서
사라지기 때문이다.

**규칙.** 설정을 저장하기 전에 물을 것: 이것은 **자판**에 대한 설정인가, **화면**에 대한 설정인가.
화면마다 다른 것이라면 화면마다 값이 있어야 하고, 예전의 값 하나에서 옮겨 올 길이 있어야 하며, 이쪽
화면에서 저쪽 화면의 값을 설정할 방법이 있어야 한다. 그리고 회전 시 다시 지을 것: 입력 뷰가 반드시
재생성된다는 보장이 없으므로, 방향이 실제로 바뀌었을 때 `onConfigurationChanged` 가 그 일을 해야 한다.

### 15.20 예측할 수 없는 것을 예측하려다 삼켜진 키

**벌어진 일.** Tab·Esc·방향키·F키를 눌러도 아무 일도 일어나지 않았다. 화면은 키가 눌린 것처럼
반응하고, 뷰는 `RAW_KEY` 를 제대로 내보내고, 실행기의 경로도 맞았다. 그런데 그 사이에서
`EditorBoundsPredictor.after` 가 `RAW_KEY` 를 모른 채 `IllegalStateException` 을 던졌고,
서비스의 "어떤 에디터도 키보드를 죽일 수 없다" 는 `catch (RuntimeException)` 이 그것을 조용히
삼켰다. 방어 장치가 결함을 감춘 것이다 — 자기 코드의 버그까지 삼키는 catch 는 침묵을 만든다.

**해결.** 예측기에 `RAW_KEY` 를 가르치되, 무엇을 예측하라는 것이 아니라 **예측할 수 없음**을
말하게 한다. Tab 은 들여쓰기일 수도 포커스 이동일 수도 있고, 방향키는 커서를 옮기며, Esc 는
아무것도 하지 않을 수 있다. 커서가 어디로 갔는지는 에디터만 안다.

```java
case PERFORM_EDITOR_ACTION:
case RAW_ENTER:
case RAW_KEY:
    return EditorBounds.unknown();
```

**규칙.** 파이프라인에 액션 종류를 새로 더할 때는, 그것을 소비하는 모든 `switch` 를 함께 찾아라.
`default: throw` 는 빠뜨린 곳을 잡아 주지만, 그 예외를 삼키는 catch 가 위에 있으면 아무것도
잡아 주지 못한다. 그리고 커서를 모른다는 답은 틀린 답이 아니다 — 틀린 위치를 아는 척하는 것이
훨씬 나쁘다.

### 15.21 유리 위에는 계속 누르고 있을 키가 없다

**벌어진 일.** Tab 은 Ctrl·Meta·Alt 옆에 "래칭 수정자" 로 놓여 있었다. 한 번 누르면 armed 집합에
들어갔다 나왔다 했지만, 그 집합을 읽는 어느 곳에도 TAB 분기가 없었고 `KeyModifier` 에도 TAB 은
없다. 눌러도 키 색만 바뀌고 탭은 입력되지 않았다. Tab 은 수정자가 아니다 — 그 자체가 글쇠다.

**해결.** 손가락이 키에 할 수 있는 일은 둘이다. 짧게 누르기는 **입력**이고, 길게 누르기는
**계속 누르고 있기**다. 그래서 Tab 은 탭하면 한 번 입력되고(누른 armed 수정자와는 화음을 이룬다),
길게 누르면 눌린 채로 잠기며, 다시 길게 누르면 놓인다.

키 하나를 내리누른 채로 두려면 누름의 반쪽만 보낼 수 있어야 한다. `RawKeyPhase` 가 그 반쪽을
나른다 — `TAP` 은 down 과 up 둘 다, `HOLD` 는 down 만, `RELEASE` 는 up 만.

```java
sink.accept(ProjectKeyEvent.softwareDown(TAB_KEY_ID, SemanticInput.rawKey(
    RawKey.TAB, EnumSet.noneOf(KeyModifier.class),
    tabHeld ? RawKeyPhase.HOLD : RawKeyPhase.RELEASE)));
```

잠긴 키는 `keyFillColor` 가 강조색으로 칠하고 — 그러려면 **그리기 캐시 서명에 잠금 상태가 들어가야
한다**. 넣지 않으면 상태는 바뀌는데 화면은 그대로다. 그리고 에디터 세션이 바뀔 때는 반드시 up 을
보낸다. 눌린 채로 남은 키는 그 키를 누른 에디터보다 오래 살아서는 안 된다.

**규칙.** 키를 "무장" 시키는 것과 "누르고 있는" 것은 다른 일이다. 무장은 다음 키를 기다리는
뷰 안의 상태이고, 누름은 에디터가 이미 들은 사실이다. 후자를 만들었으면, 그것을 되돌릴 길
(다시 길게 누르기)과 새어 나가지 않을 길(세션 종료 시 해제)을 같이 만들어라.

### 15.22 한 개까지만, 아니면 계속

**벌어진 일.** Shift 는 오래전부터 세 상태였다 — 꺼짐, 한 글자만, 그리고 잠금. Ctrl·Meta·Alt 는
아니었다. 집합 하나에 들었다 나왔다 하는 두 상태뿐이어서, Ctrl 을 여러 번 쓰려면 키마다 다시
눌러야 했다. 같은 개념이 두 벌로 구현되어 한쪽만 자란 것이다.

**해결.** 상태를 `LatchState` 로 뽑아 Shift 와 수정자가 같은 것을 쓴다. 탭은 한 개만 무장하고,
길게 누르면 잠기며, 잠긴 것을 탭하면 풀린다 — 잠금에서 빠져나오는 길은 가장 찾기 쉬워야 한다.
차이는 키가 눌릴 때만 드러난다: 화음은 **활성인 것 전부**를 싣고, 그 뒤 **한 개짜리만 소모된다**.

```java
public boolean consumeOneShots() {   // ModifierLatches
    boolean changed = false;
    for (ControlKey modifier : KEYS) {
        changed |= latch(modifier).consumeOneShot();   // 잠긴 것은 소모되지 않는다
    }
    return changed;
}
```

**표시.** 배경색만으로는 "한 개만"과 "계속"이 구분되지 않는다. 그래서 키 우상단에 도형을 하나
둔다 — **속 빈 고리는 잠기지 않음, 꽉 찬 점은 잠김.** 배경은 같은 말을 색으로 한 번 더 한다
(연한 강조 = 무장, 진한 강조 = 잠김). 하나의 사실을 두 채널로 말하는 것이라, 색이 닿지 않는
사람에게도 모양이 남는다. 이 표시는 Tab 과 Shift 도 같이 쓴다. 셋이 같은 일을 하는 키이기 때문이다.

**규칙.** 같은 상태 기계를 두 번째로 쓰게 되거든, 두 번째를 새로 쓰지 말고 첫 번째를 꺼내라.
그리고 상태가 셋이면 표시 채널도 둘이어야 한다 — 두 상태는 색 하나로 되지만, 셋은 안 된다.

### 15.23 세 상태를 색 하나로 말하려던 것

**벌어진 일.** 수정자의 "한 개만"은 거의 흰색에 가까운 연한 파랑이었다. 켜져 있는지 아닌지가 한눈에
안 들어왔고, "계속"과 나란히 놓아도 둘 다 파랗다는 것 말고는 구별이 어려웠다.

**해결.** 두 상태에 각각 제 색을 준다 — 무장은 중간 파랑, 잠금은 훨씬 진한 파랑. 그런데 색을 진하게
하는 순간 **글자가 배경에 잠긴다.** 라이트 테마의 잠금색은 어둡고, 다크 테마의 잠금색은 오히려 밝다.
테마마다 글자색을 손으로 정하면 넷 중 하나는 반드시 틀린다.

그래서 잉크를 배경의 **휘도에서 정한다.** 0.179 는 sRGB 검정과 흰색이 같은 대비를 내는 지점이고,
취향으로 옮길 값이 아니다.

```java
public static boolean prefersDarkInk(int red, int green, int blue) {
    return relativeLuminance(red, green, blue) > 0.179;
}
```

`KeyLabelContrast` 는 안드로이드에 기대지 않는다 — 묶인 색이 아니라 채널 셋을 받으므로 기기 없이
시험할 수 있고, 네 가지 래치 색이 각각 어느 잉크를 받는지가 테스트로 고정된다.

**규칙.** 상태가 셋이면 색 하나로는 말할 수 없다. 그리고 배경색을 바꾸는 변경은 **글자색을 함께
바꾸는 변경**이다. 대비를 눈으로 정하지 말고 계산하라 — 눈은 자기가 방금 고른 색에 관대하다.

### 15.24 쓰는 키와 읽는 키가 달랐다

**벌어진 일.** 키보드 높이가 조절되지 않았다. 메뉴의 크기 버튼도, 설정 화면의 슬라이더도 값을
바꾸기는 하는데 화면은 그대로였다. 방향별 설정이 들어오면서 쓰기는 `height_scale.portrait` 로
갔는데, 설정이 바뀔 때 다시 읽는 쪽은 예전의 `height_scale` 를 그대로 보고 있었다. 그 키는
존재하지 않으니 **기본값이 돌아왔고**, 값을 바꾼 그 순간 원래대로 되돌려졌다.

```java
// 틀림: 이 키는 아무도 쓰지 않는다
prefs().getFloat(KEY_HEIGHT_SCALE, DEFAULT_SCALE)
// 맞음: 이 방향이 실제로 쓰는 키
OrientedPrefs.getFloat(prefs(), KEY_HEIGHT_SCALE, orientation(), defaultHeightScale())
```

설정을 방향별로 쪼갤 때 쓰기는 한 곳에서 바꾸면 끝이지만, **읽기는 여러 곳에 흩어져 있다.**
여기서는 생성자와 설정 변경 콜백 둘이 있었고 한쪽만 고쳐졌다.

**기본값도 같이 고쳤다.** 밀도로 계산한 고정 높이는 화면이 달라지면 의미가 달라진다. 이제 화면
**긴 변의 25%** 다 — 가로에서도 긴 변을 쓰므로, 기기를 돌려도 손에 잡히는 크기가 같다. 짧은 변의
25% 로 하면 가로 자판이 띠처럼 납작해진다.

**규칙.** 설정 하나에 키가 둘 생기면, 그 설정을 **읽는 모든 곳**을 찾아라. 그리고 기본값이 화면에
의존한다면 기본값을 계산하는 자리도 한 곳이어야 한다 — 키보드와 설정 화면이 서로 다른 기본값을
믿으면 슬라이더는 사용자가 보고 있지 않은 숫자에서 시작한다.

### 15.25 오토마타가 본 적 없는 순서로 손가락이 떨어진다

**벌어진 일.** 빨리 칠 때 획추가가 가끔 먹지 않았다: 자음은 민자로 찍히고 획은 사라졌다. 키는
손끝이 옆 키로 미끄러질 수 있도록 **뗄 때** 입력되는데, 빠른 롤 타이핑에서는 앞 손가락이 떨어지기
전에 다음 손가락이 내려오고, 두 손가락은 어느 쪽이 먼저든 떨어질 수 있다. 획추가 손가락이 먼저
떨어지면 인터프리터는 획-다음-자음 순서를 본다: 획은 작용할 대상이 없어 아무것도 답하지 않고,
자음은 그대로 찍힌다. 자모표도 컴포저 테스트도 전부 옳았다 — 결함은 터치 계층이 먹여 주는
순서에 있었고, 오토마타 단위 테스트로는 영원히 닿을 수 없는 곳이었다.

```java
// beginTouch: 새 손가락이 내려오면, 아직 앞 손가락에 실려 있는 글자를 전부 확정한다.
settlePendingTaps();   // 누른 순서대로 입력하고, 그 손가락의 뗌은 소진시킨다
```

이미 동작한 손가락 — 홀드, 반복, 열려 있는 플릭 안내 — 은 건드리지 않고, 컨트롤 키 위의
손가락도 그대로 둔다: 모디파이어 코드는 애초에 두 손가락이 함께 내려와 있는 상태이므로, 확정해
버리면 코드에서 모디파이어를 빼앗게 된다. 확정된 누름은 소진된 제스처들이 쓰는 그 플래그
`holdConsumed` 를 세우므로, 그 손가락의 뗌은 기존의 조기 반환으로 걸어 들어가 아무것도 더
입력하지 않는다.

**누른 순서에 왜 별도 카운터가 필요한가.** 터치 맵은 포인터 id 로 키가 잡히는데, 포인터 id 는
재활용된다: 두 손가락이 내려와 있을 때 다음 누름이 아직 잡혀 있는 것보다 *작은* id 를 받을 수
있다. 맵 순서로 확정하면 고치려던 롤 버그를 그대로 재연하므로, 각 터치는 단조 증가 카운터에서
받은 순번을 지니고, 확정은 그 순번으로 정렬한다.

**규칙.** 뗄 때 입력하는 키보드 뒤의 상태 기계에는 누른 순서로 먹여야 하고, 누른 순서를 아직
알고 있는 유일한 순간은 다음 손가락이 내려오는 때다. 대기 중인 탭은 거기서 확정한다. 그리고
*먹이는 쪽*을 테스트하라 — 인터프리터와 컴포저는 이 결함이 발견되기까지 내내 결백했다. 모든
테스트가 사용자가 누른 순서 그대로 키를 건네주고 있었기 때문이다.

### 15.26 기억이 유일한 사본인 오토마타

**벌어진 일.** 획추가·쌍자음이 여전히 가끔 무반응이었다: 나랏글 인터프리터는 자기가 *기억하는*
방금 친 글자에 작용하는데, 그 기억은 사용자가 타이핑과 연결짓지 못하는 일들 — 에디터의 입력
세션 재시작, 하드웨어 키, 레이어 전환 — 로 지워진다. 글자는 화면에 멀쩡히 있는데, 그것을
변환할 키만 그 글자를 잊은 것이다.

**해법은 기억을 늘리는 게 아니라 폴백이다.** 인터프리터가 변환 키에 아무 답도 못 하면 그 누름을
`TRANSFORM` 시맨틱 입력으로 넘기고, 프로세서가 실제 화면 위의 것을 상대로 2단으로 해석한다:

```java
// 1단: 조합 중인 음절이 있다 — 그 끝 자모가 대상이다.
int consonant = composer.trailingConsonant();   // 단독 초성, 또는 받침(겹받침이면 꼬리)
// 2단: 조합 중인 것이 없다 — 커서 앞 한 글자를 읽어 마지막 자모를 변환하고,
// 결과를 다시 조합 중 음절로 되살려 타이핑이 이어지게 한다.
```

2단은 라이브 동작을 그대로 재현한다: 받침은 제자리에서 변환되고(각 → 갘), 받침이 될 수 없는
쌍자음은 음절을 가른다(갇 → 가ㄸ). 겹받침은 꼬리를 풀고(많 → 만ㅇ), 받침 없는 음절은 모음이
이오테이션된다(가 → 갸). 그리고 결과는 컴포저에 시딩되어 다시 *조합 중*이므로, 다음 모음이
받침을 그대로 끌고 간다(갘 + ㅏ → 가카). `getTextBeforeCursor`에 답 못 하는 에디터(터미널)는
그 누름이 그냥 무동작이 된다 — 원래도 그랬듯이.

**규칙.** 오토마타의 유일한 상태가 "방금 쓴 것의 기억"이면, 그 기억의 모든 리셋이 죽은 키가
된다. 모든 변환에 문서 자체에서 대상을 다시 끌어낼 길을 주라. 그러면 기억은 필수가 아니라
최적화가 된다.

### 15.27 커서를 따라가는 조합

**벌어진 일.** 음절을 조합하는 중에 터치나 마우스로 커서를 다른 곳으로 옮기면, 치다 만 그
음절이 가끔 새 위치에 나타났다. 컴포저는 커서를 보지 않으므로 이동 후에도 상태를 그대로
들고 있었고, 다음 키의 `setComposingText`가 그 묵은 음절 전체를 지금 커서가 있는 곳에
그려 넣은 것이다.

**이 수정이 피해야 했던 함정.** 자명해 보이는 규칙 — "예측과 다르게 선택이 바뀌었으니 리셋" —
은 한 번 시도됐고(v0.1.11) 키보드를 죽였다: 실제 에디터는 예측 위치에 정확히 내려앉지 않아
우리 자신의 키 입력이 외부 이동으로 읽혔고, 매 키마다 컴포저가 리셋됐다. 하루 만에 되돌렸다
(v0.1.12).

**오발이 불가능한 신호.** `onUpdateSelection`은 에디터 자신의 조합 영역
(`candidatesStart..End`)도 보고한다. 키보드가 조합 중일 때 키보드 자신의 편집은 언제나 커서를
그 영역 끝에 남긴다 — 그러므로 영역 밖에 놓인 새 선택은 사용자만 만들 수 있다:

```java
CursorMovePolicy.shouldAbandonComposition(composing, newSelStart, newSelEnd,
    candidatesStart, candidatesEnd)
// true → finishComposingText()(음절은 있던 자리에 정착), 컴포저 리셋, 12키 런 종료.
// 다음 키는 새 위치에서 깨끗하게 시작한다.
```

영역 없는 보고는 건드리지 않는다 — 커밋의 중간 상태가 그렇게 보이고, 조합 텍스트를 표시하지
않는 에디터의 모든 보고도 그렇다. 거기서 리셋하면 그런 에디터에서 조합이 한 타마다 깨진다.
그런 에디터는 새 버그를 얻는 대신 (드문) 기존 동작을 유지한다.

**영역을 아예 보고하지 않는 에디터.** Compose 텍스트 필드 — 구글 Keep이 그렇다 — 는 조합
중에도 `candidatesStart = -1`을 넘기므로 영역 검사가 거기서는 영영 발화하지 않았고, 묵은
조합이 살아남았다: 탭 후 타이핑이 커서를 곧장 옛 자리로 끌고 갔다. 정확히 그런 보고에만 두
번째 판정을 둔다. 여전히 예측 없이 — 키보드가 조합 중일 때 키보드 자신의 (배치된) 편집은
언제나 커서를 프리에딧 바로 뒤에 남기므로, `getTextBeforeCursor(프리에딧 길이)`는 프리에딧
그대로 읽혀야 한다. 거기에 다른 것이 있다면 — 또는 조합이 결코 만들지 않는 범위 선택이라면 —
그것은 사용자의 손이다. null 읽기는 영역 미보고와 똑같이 조합을 건드리지 않는다.

**규칙.** "사용자가 커서를 옮겼는가"는 에디터가 단언하는 증거 — 에디터 자신의 조합 영역,
그것이 없으면 에디터 자신의 텍스트 — 로만 판정하라. 예측한 위치와의 비교로는 절대 판정하지
말라. 그리고 조합을 버릴 때는 먼저 있던 자리에 정착시켜라. 텍스트는 커서를 따라 이동해서는
안 된다.

**구간 시험은 그래도 약했다.** "구간 바깥"이라는 물음은 구간의 시작점에 놓인 커서를 우리 것으로
통과시켰고, 한 글자짜리 조합에서 그 자리는 곧 "글자 바로 앞"이다 — 15.28을 볼 것.

### 15.28 "조합 중인 글자 안"은 손가락이 닿을 수 있는 자리가 아니다

**벌어진 일.** 바다가자를 치고 커서를 마지막 글자 바로 앞으로 옮긴 뒤 백스페이스를 누르면, 커서
자리가 아니라 맨 끝에서 지웠다 — 바다가ㅈ. 딱 그 자리에서만 그랬다. 커서를 다른 곳에 한 번
들렀다 오면 정상이었다. (이슈 #5 신고, 한 판 앞서 이슈 #2 댓글에서 먼저 언급됨.)

**왜 하필 그 자리인가.** 15.27의 규칙은 새 선택이 편집기의 조합 구간 *바깥*인지를 묻는다. 그런데
자는 아직 `3..4`에서 조합 중이었고, 그 바로 앞을 누르면 커서는 3 — 구간의 시작점 — 에 놓인다.
바깥이 아니다. 그래서 조합이 유지됐고, `HangulInputProcessor.delete()`는 백스페이스를 조합기에
넘겼으며, 조합기는 자를 풀어 그 결과를 구간 위에 다시 썼다. 커서는 물어보지도 않았다.
`setComposingText`는 커서가 어디 있든 늘 조합 구간을 갈아치우기 때문이다.

이 규칙은 "가운데를 눌러 들어갈 수 있는 조합 문자열"을 머릿속에 두고 쓴 것이다. **한글의 조합
문자열은 한 글자다.** 가운데가 없다. 그 안의 모든 자리는 끝(우리 편집이 커서를 두는 자리)이거나
시작(사용자가 커서를 옮겨야만 닿는 자리)이다.

**고친 방법.** 약한 질문 대신 정확한 질문을 하게 했다. 조합은 커서를 구간 끝에 점으로 놓고 범위를
선택하는 일이 없으므로, 그것만이 우리 편집이 만들어 내는 보고다.

```java
// 전: 구간 바깥인가?
return newSelStart < candidatesStart || newSelStart > candidatesEnd
    || newSelEnd < candidatesStart || newSelEnd > candidatesEnd;
// 후: 우리 편집이 두었을 바로 그 자리인가?
return newSelStart != newSelEnd || newSelStart != candidatesEnd;
```

문자열이 긴 조합(Telex, 로마자)에서 진짜로 가운데를 누를 수 있는 경우의 같은 구멍도 함께 막힌다.

**규칙.** 포함 관계를 시험할 때는, 사용자가 드나든다고 상상한 구간이 아니라 **내 코드가 실제로
만들어 내는 자리**를 이름 붙여 그것과 견주어라. 15.27은 이 결함의 거울이다. 그쪽은 조합을 너무
쉽게 버려 하루 만에 되돌려야 했고, 이쪽은 너무 오래 붙들었으며 몇 달이 걸려서야 드러났다. 붙들고
있는 쪽은 "아직 우리 것"으로 읽히는 그 한 경계에 커서가 닿기 전까지는 아무 일도 없어 보이기
때문이다.

### 15.29 주인이 둘인 래치

**벌어진 일.** 액션 바의 Ctrl/Alt/Meta 슬롯은 패드의 수식키처럼 길게 누르면 잠긴다. 다시 눌러 풀면
수식키 자체는 풀렸지만, 슬롯은 잠긴 색으로 남았고 그 뒤로는 다시 잠글 수도 없었다.

**왜.** 같은 사실을 두 객체가 들고 있었다. 진짜 상태는 키보드 뷰의 `ModifierLatches` 가 갖고,
`ActionBarView` 는 슬롯을 칠하고 다음 홀드를 잠글지 정해야 해서 `latched` 집합을 따로 둔다. 잠긴
빌트인을 누르면 `listener.onAction(...)` 으로 가서 키보드의 래치를 톡 친다 — 여기까지는 맞고 수식키는
풀린다 — 그런데 바에게는 아무도 알려 주지 않아 그 집합에는 슬롯이 그대로 남는다. 그때부터 `paint()`
는 무조건 강조색을 칠하고 `press()` 는 새 잠금을 걸지 않는다. 둘 다 `latched.contains(slot)` 를 묻기
때문이다.

**고친 방법.** 잠긴 슬롯을 푸는 누름은 `setLatched(false)` 를 지나가게 했다. 그것만이 양쪽을 함께
바꾸는 길이다 — 바의 집합을 고치고 `onChordLatch(slot, false)` 를 불러 키보드의 잠금을 푼다.
`onAction` 은 지금 눌려 있지 않은 슬롯의 몫으로 남긴다.

**규칙.** 제 것이 아닌 사실을 두 번째 객체가 들고 있어야 한다면, 들어가는 길과 나오는 길을 **하나씩만**
두고 모든 누름을 그리로 보내라. 두 갈래로 지울 수 있는 래치는 결국 프로그램의 절반에게만 말하는 쪽으로
지워진다 — 그리고 풀리지 않는 수식키는 사용자에게 "누른 채 놓을 수 없는 키"로 보인다.

### 15.30 터미널은 네 번째 종류의 편집기다

**벌어진 일.** Termux 에서 글자 입력이 두 모드 모두 망가져 있었고, 증상이 서로 달라 다른 결함처럼
보였다. `enforce-char-based-input = true` 에서는 한글 음절이 **끝나야만** 나타났고 백스페이스는 아무
일도 하지 않았다. 끄면 백스페이스는 되는데 **한글이 아예 안 들어갔다**. (이슈 #7.)

**하나의 모양, 두 개의 변장.** Termux 는 첫 모드에서 `TYPE_NULL` 을, 둘째 모드에서는 제안 없는 평범한
`TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` 필드를 보고한다. 둘 다 실제를 말하지 않는다. 그 뒤에 있는 것은
파이프 건너편의 프로그램이다. 텍스트 뷰가 없으니 **조합 구간이 없고**(IME 가 조합으로 표시한 것은
커밋되기 전까지 보이지 않는다), **버퍼가 없으니** 주변 텍스트 삭제는 연결의 더미 `Editable` 에 닿고
거기서 멈춘다. 이 키보드는 편집기를 세 종류로 알고 있었다 — 서식 있는 텍스트, "키를 보내라"는
TYPE_NULL, 그리고 원격 데스크톱. 터미널은 **네 번째**이고, 셋 다 각자 다른 방식으로 실패했다.

**증상 하나에 거절 하나, 모두 셋.**

1. *조합이 화면에 닿지 않았다.* 조합 구간이 없는 편집기를 위해 조합을 **커밋으로 바꿔 내보내는** 길은
   이미 있었지만(원격 데스크톱), 그것이 켜지는 조건이 **패키지 이름 넷**뿐이었다. `TYPE_NULL` 은 그것을
   켜지 않아 `setComposingText` 가 매번 거절됐고 음절을 닫는 커밋만 통과했다. **→ "음절이 끝나야
   보인다".**
2. *삭제가 거절됐고, 통과해도 닿지 않았다.* 커밋으로 그린 음절을 고치려면 **화면에 넣은 것을 도로
   빼고 새것을 커밋**해야 한다 — 한 계획에 두 동작. 그런데 원시키 편집기는 짧은 목록에 든 **동작
   하나짜리** 계획만 받았고 그 목록에 되감기가 없었다. 게다가 되감기는
   `deleteSurroundingTextInCodePoints` 를 썼는데, 터미널은 그것을 보지 못한다. **→ "백스페이스가
   안 먹는다".**
3. *비밀번호 필드에는 조합을 하지 않았다.* 사적인 필드로 표시되면 조합을 아예 거절했다. 터미널은
   제안을 끄려고 그 변형을 보고하고, "비밀번호 보기"를 켠 모든 로그인 창도 마찬가지다. **→ "한글이
   아무것도 안 된다" — 그리고 이 앱에서 한글은 어떤 비밀번호 칸에도 칠 수 없었다.**

**고친 방법.** 터미널을 모델에 이름으로 넣었다 — `EditorCapabilities.asTerminal()`: 조합 구간 없음,
버퍼 없음, 삭제는 키 이벤트. `TYPE_NULL` 은 정의상 그것이고, 텍스트 필드를 보고하는 터미널은 패키지
이름으로 알아본다. `EditorInfo` 안에는 그것을 로그인 창과 갈라 줄 것이 없기 때문이다. 커밋과 삭제로만
이루어진 계획은 통째로 받아들인다. 그리고 "사적이다"는 원래 뜻으로 돌아갔다 — **읽지 않는다, 기억하지
않는다**. 조합은 애초에 새는 곳이 아니었다.

**이름 목록도 충분하지 않았다.** 첫 수정은 텍스트 칸이라고 알리는 터미널을 **패키지 이름**으로
알아봤다. 제 유닛 시험은 다 통과하고, 아무도 목록에 넣지 않은 첫 터미널에서 그대로 실패한다. 모든
터미널이 공통으로 가진 것은 이름이 아니라, **커서가 어디인지 모른다**는 사실이다 — 커서가 있을 버퍼가
없으니까. 반면 글자 칸은 언제나 안다. `initialSelStart` 가 -1 인 것과 "비밀번호 보기 + 제안 없음"
모양을 함께 보면 **앱이 아니라 종류**를 짚게 된다. 이것은 진짜 프레임워크 건너편에 터미널 모양의
편집기를 놓고 실제로 쳐 넣어 보다가 찾았다(계측 빌드의 `TerminalHostActivity`). 유닛 시험은 초록이었고
기기는 아니었다.

**진짜 Termux 가 보고하는 것.** 0.118.3, 안드로이드 13에서 실측: 터미널 뷰는 `inputType=0x0`
(TYPE_NULL)과 `initialSelStart=-1` 을 보고하며, **`~/.termux/termux.properties` 에
`enforce-char-based-input=false` 를 써 넣고 앱을 다시 띄워도 여전히 TYPE_NULL 이다.** 사용자가 보는 두
모드가 언제나 두 개의 `EditorInfo` 모양인 것은 아니다. 비밀번호 보기 모양을 보고하는 빌드도 있고(신고의
나머지 절반이 그것이다) 이 빌드는 아니다. 그래서 모양 규칙이 중요하고, 이름 목록은 여기서는 통했겠지만
다른 데서는 쓸모없었을 것이다.

**물리 키보드, 남겨 두었던 절반.** 터미널에서는 물리 키를 전부 그대로 통과시켰다 — RAW_KEY 편집기면
`onKeyDown` 이 일찍 돌아갔고 `applyHardwareMode` 는 변환기를 주지 않았다. Ctrl-C 와 방향키가 프로그램에
닿게 하려던 것인데, 그 때문에 블루투스·USB 키보드의 한글은 조합기에 가지도 못했다. 이제 터미널도 다른
편집기와 같은 변환기를 받는다(`TerminalHardwareKeys`). 현재 자판이 바꾸는 키 — 한글 글자, 콜맥 글자 —
는 조합기를 거쳐 화면 키처럼 확정 입력으로 쓰이고, 어느 변환기도 맡지 않는 키(Ctrl 조합, 방향키, Enter,
Esc, Tab, 한글 모드의 스페이스)는 여전히 손대지 않고 통과한다. QWERTY 의 영어는 바꾸는 키가 없으니 예전
통과를 그대로 쓴다. 함정이 하나 따라왔다. 통과시키는 키 앞에서는 **음절을 먼저 끝내야**
한다(`endSyllableBeforeDelegating`). 음절은 이미 건너편에 있으니 끝내도 쓰는 것은 없다. 하지만 이것이
없으면 가 + 스페이스 + ㄴ 에서 가를 간으로 다시 그리려고 한 글자를 도로 빼는데, 그 한 글자가 스페이스다.
조합을 같은 방식으로 확정 입력으로 쓰는 원격 데스크톱 창에도 똑같이 적용된다. `TerminalHardwareKeyTest`
가 그 위험과 수정을 함께 기록한다.

**도로 빼기가 엉뚱한 통로를 탔다(0.1.165).** 0.1.162 의 기기 확인은 진짜 Termux 에 두 음절을 쳐 보고
통과했다. 여덟 음절 — 한글입력대한민국 — 은 화면 키로도 물리 키로도 11번 중 0번 온전했다. 다시 그리기는
한 계획 안의 도로 빼기와 확정이고, 도로 빼기는 백스페이스 **키 이벤트**로 나갔다. 키 이벤트는 뷰의 입력
이벤트 줄을 거치지만 `commitText` 는 터미널에 곧바로 쓰인다(Termux 의 `sendTextToTerminal`). 그래서
확정이 키를 앞질렀고, 늦게 온 백스페이스가 그 확정을 지웠다. 이제 도로 빼기는 터미널 자신의 지우기 문자
— DEL, `0x7f`, Termux 의 백스페이스 키가 보내는 바로 그 바이트 — 를 뒤따르는 음절과 같은 통로로 글자로
확정한다(`executeTerminalErase`). 11번 중 11번 온전했다. 사용자가 혼자 누른 백스페이스는 여전히 키
이벤트다. 그 계획에는 뒤따르는 것이 없고, 진짜 키여야 터미널이 자기 지우기 바이트를 고른다.
`deleteSurroundingText` 는 탈출구가 아니다. Termux 가 그것을 같은 키 이벤트로 바꾼다. 옛 경로는 도로
빼기 개수를 앞선 연산 수 자리에 넘겨서 늘 백스페이스 하나만 보냈다 — 한 코드 포인트짜리 음절에는 무해했지만
텔렉스 단어에는 틀렸다. **규칙:** 터미널은 긴 단어로 시험하라. 경주하는 두 통로는 부하가 걸려야 진다.

**터미널 앱이 곧 터미널은 아니다(0.1.166).** Termux 의 부가 키 줄에는 평범한 `EditText` 가 있고
`com.termux` 는 이름 목록에 있으니, 그 앱의 모든 칸이 터미널로 분류됐다. 도로 빼기가 키 이벤트였을
때는 그 칸이 키를 먹어 치워 아무도 몰랐다. DEL 글자로 바뀌자 칸이 그 문자를 그대로 간직해서, 바다가 가
ㅂ␡바␡받␡바다… 로 남았다(0.1.165 에 대한 신고, 신고자의 두 폰 모두). 이제 이름 목록도 모양 규칙과 같은
신호를 함께 본다. 목록에 있는 패키지라도 편집기가 **커서를 알리지 않을 때만** 터미널이다. 커서 자리를
말하는 칸은 어느 앱에 있든 버퍼가 있다. **규칙:** 앱 단위 규칙도 결국 **편집기가 무엇인지**를 물어야
한다. 한 가지인 앱은 대개 다른 것도 품고 있다.

**거기서는 음절이 마냥 기다릴 수 없다(0.1.167).** 원격 데스크톱에서 소유자가 음절을 만들다 포인터를
옮기고 이어 쳤더니, 음절이 새 자리에 다시 쓰였고 그 자리에 있던 글자 하나가 먹혔다. llsant 의 Termux
화살표 신고와 같은 모양이다. 실수가 아니라 구조에서 온다. 확정 입력으로 쓰는 음절은 다시 그리려고 열어
두는데, 커서는 이 키보드에 보고가 닿지 않는 곳에서 움직였다. 터미널은 커서를 아예 알리지 않고, 원격
클라이언트의 가짜 버퍼는 제 확정의 메아리만 알린다. 그래서 이런 편집기에서는 커서 이동 판단을 꺼
두었다(64f1b60. 다시 켜니 일 이 이ㄹ 로 도착했다). 신호가 없으니 남은 손잡이는 시간이다.
`IdleSyllableSettle` 이 입력 없이 1500ms 가 지나면 그 권리를 놓는다. 쓰는 것은 없다 — 글자는 이미
건너편에 있다 — 그래서 글은 움직이지 않고, 도로 뺄 권리만 끝난다. 대가는 터미널·원격에서 쉬었다가 음절을
이어 붙일 수 없다는 것이다. 일반 편집기는 그대로다. 조합 구간이 있고 커서 이동을 알려 주니 시계가 필요
없었다.

**그리고 원격 데스크톱에는 시계가 필요 없었다(0.1.168).** 거기서 커서 이동 판단을 꺼 둔 근거는 측정이
아니라 짐작이었다 — 원격 클라이언트의 가짜 버퍼는 잡음만 낸다는 것. 에뮬레이터에서 Microsoft 클라이언트
(Windows App 11.0.26071.13915)로 진짜 Windows 11 VM 에 붙여 재 보니 또렷하게 알린다. 치는 동안에는 확정
하나마다 선택 위치가 1, 2, 3, 4 로 한 칸씩 나아가고, 12초를 가만히 둬도 저절로 움직이지 않는다. 원격
화면을 클릭하면 0 으로 돌아가는데, 그건 우리 키가 한 일이 아니다. 예전 시도가 빗나간(일 이 이ㄹ 로) 이유는
보고를 **현재** 커서와 비교했기 때문이다. 첫 보고가 오기 전에 두 키를 칠 수 있으니 메아리가 도약처럼
보인다. `MaterializedCursorMoves` 는 대신 **예상값 줄**과 비교한다 — 각 쓰기가 커서를 어디에 두었어야
하는지 — 그중 하나와 맞으면 아무리 늦게 와도 메아리다. 아무것과도 안 맞으면 사용자다. 그 VM 에서 확인:
바 를 조합하다 다른 곳을 클릭하고 ㅌ 을 치면 바 는 제자리에 남고 ㅌ 이 새로 시작한다. 클릭하지 않으면
같은 두 키가 여전히 밭 으로 합쳐진다. 터미널에는 이런 신호가 없다 — 커서를 아예 알리지 않는다 — 그래서
거기서는 1.5초 시계가 유일한 손잡이로 남는다.

**그리고 터미널에서는 도로 빼기 자체를 없앴다(0.1.169).** 위의 터미널 결함 전부 — 확정에 추월당한
키, 글자 칸에 남은 DEL, 신고자가 본 깜빡임, 음절을 끌고 가는 방향키 버튼 — 는 한 가지 선택의 값이다.
조합 구간을 가질 수 없는 곳에 음절을 그려 넣고, 자모마다 도로 빼는 것. 터미널은 조합에서 아예 빼 둘 수
있다. `EditorCapabilities.composingOffScreen()` 이 편집기에 표시를 달고, `HangulInputProcessor` 는 모든
계획에서 `SET_COMPOSING_TEXT` 와 `FINISH_COMPOSING` 을 걷어 내며(원시 키 편집기는 둘 다 거절하고,
거절된 호출 하나가 계획 전체를 끌고 내려간다) **닫힌 것만** 보낸다. 만들고 있는 음절은
`ComposingStripView` — IME 의 *candidates view* — 에 보인다. 떠 있는 패널이 아니라 이것을 고른
이유는, 안드로이드가 키보드 자체가 숨어 있어도 candidates view 는 화면에 올리기 때문이다. 물리
키보드를 꽂은 경우가 바로 그렇다. 도로 빼는 것이 없으니 보이지 않는 커서 이동이 엉뚱한 자리로 옮길 것도
없고, 1.5초 시계도 거기서는 걸리지 않는다. 바, 쉼, ㄷ 이 다시 받 이 된다. 뜻이 바뀌는 것은 "음절이 이미
저쪽에 있다"고 가정하던 모든 경로다. 음절을 끝내는 일 — 그대로 넘기는 물리 키 앞, 칸을 떠날 때, 언어를
바꿀 때, 한글을 끌 때 — 은 이제 음절을 **써야** 한다(`commitSyllableHeldOnTheStrip`). 아니면 버려진다.
떠 있는 키보드의 터치 영역은 이제 창 안에서의 판 위치만큼 옮겨 잡는다. 입력 뷰 위에 띠가 붙으면 판이
그만큼 내려가기 때문이다. 옛 방식(터미널에 직접 그리기)은 설정(일반 페이지의 **터미널**)으로 남겼다.
음절이 터미널 안에 보이는 것을 신고자가 좋게 봤기 때문이다. 에뮬레이터의 Termux 0.118.3 에서 잰 것:
여덟 음절 단어 11/11 (키당 120·300 ms), ㄱㅏㄴ + 백스페이스 → 가(DEL 바이트 0), 물리 키 가 Space 나 →
`가 나`, 음절 중간에 홈 화면으로 떠났다 오기 8번 중 7번 보존 — 첫 시도의 유실 한 번은 재현되지 않았고,
앱이 이미 닫은 연결이면 같은 식으로 잃는다. 에뮬레이터가 보여 줄 수 없는 것: 띠 자체(그곳 IME 창에는 그릴
표면이 없다), 그리고 띠가 떠 있을 때 떠 있는 키보드의 터치.

**그리고 띠가 보이지 않았다(0.1.170).** 신고자가 0.1.169 를 두 휴대폰에서, 붙인 모드와 떠 있는 모드 모두로 써 봤다. 완성된 음절만 들어갔고, 띠는 어디에도 없었다. 에뮬레이터에서 통과한 것은 처음 잰 순간에 창이 우연히 띠가 보이는 상태로 만들어졌기 때문이다. 키가 떠 있는 상태에서 IME 창의 뷰 트리를 찍어 보니 원인이 보였다. `ComposingStripView` 와 그 candidates 틀은 글자를 담은 채 `VISIBLE` 이었지만, 그 부모인 프레임워크의 전체 화면 영역이 `INVISIBLE` 이었다. `InputMethodService.updateExtractFrameVisibility()` 는 창이 배치되는 **그 순간의** candidates 가시성으로 그 영역을 맞추고, 나중의 `setCandidatesViewShown(true)` 는 안쪽의 candidates 틀만 바꾼다. 고친 코드는 띠를 보일 때 띠에서 위로 올라가며 숨은 부모를 모두 보이게 한다. 같은 덤프가 두 번째 비용도 보여 줬다. 숨김의 기본값은 자리를 차지하는 `INVISIBLE` 이라, 0.1.169 부터 모든 앱에서 키 위에 56 px 빈 줄이 있었다. 이제 `getCandidatesHiddenVisibility()` 가 `GONE` 을 돌려준다. 떠 있는 모드는 아예 다른 답이 필요했다. 창이 화면 전체를 덮으므로 candidates 띠는 화면 맨 위 상태 표시줄 밑에 붙었고, 띠가 나타나고 사라질 때마다 아래 판이 56 px 줄었다 늘며 키 크기가 바뀌었다. 거기서는 음절을 떠 있는 판 자신의 제목 줄, 건너가기 키와 닫기 키 사이에 그린다(`FloatingKeyboardFrame.setComposingText`). candidates 띠는 붙인 키보드와, 물리 키보드로 키가 숨은 경우에 쓴다. 에뮬레이터에서 IME 창의 decor 뷰를 비트맵에 그려 붙인 모드와 떠 있는 모드 둘 다 확인했다.

**규칙.** 한 앱에서 증상 둘이 서로 다른 결함처럼 보이면, 먼저 **그 앱이 무엇인지**를 물어라. 어떤
기능이 **패키지 이름 목록**으로 켜지고 있다면, 그 목록이 무엇을 대신하고 있는지 물어라 — 여기서 그것은
"조합 구간이 없다"를 대신하고 있었고, 그런 편집기는 둘 더 있었다. 그리고 **무언가를 알아보는 것에
기대는 수정은, 알아보지 못하는 것을 상대로 시험하라.**

### 15.31 키를 흘려보내는 키보드 자신의 패널

**벌어진 일.** 노트는 키보드가 앱 위에 그리는 패널이고 자기 글자 칸을 가진다. 노트가 열려 있는 동안
서비스는 글자·자모·백스페이스·엔터만 노트로 보냈고, 나머지는 보내지 않았다. 소프트 Ctrl 로 만든
조합, 액션바의 방향키와 Home/End, 누르고 있는 Tab, 액션바의 모두 선택·단어·잘라내기·복사·붙여넣기가
모두 노트를 지나 뒤의 앱으로 갔다. Ctrl+C 는 사용자가 볼 수도 없는 칸에서 복사했고, 액션바의
방향키는 아무도 보지 않는 커서를 옮겼다(0.1.172).

**고친 방법.** 패널이 열려 있는 동안에는 모든 입력 종류가 패널의 것이다. `NotepadKeys` 가 원시 키와
수정자를 명령으로 읽고(Ctrl+A/C/X/V/Z/Y, Shift+Insert, 방향키, Ctrl+방향키 단어 이동, Home/End,
페이지 키, Tab, Delete), `NotepadView.applyKey` 가 포커스된 칸에서 실행하며 — Shift 는 선택을 늘린다
— 액션바의 컨텍스트 메뉴 명령은 `NotepadView.editCommand` 로 간다. 패널에 쓸모없는 키는 넘기지 않고
삼키며, 길게 누르기는 한 번 동작하고 뗌은 아무 일도 하지 않는다.

**규칙.** 키보드의 글자를 받는 패널은 전부 받아야 한다. 디스패처가 만들 수 있는 입력 종류를 늘어놓고
각각을 패널이 어떻게 할지 정하라. "나머지는 앱으로"가 패널이 새는 길이다.

### 15.32 한 곳에서만 읽고, 첫 반복에서 소진되는 수정자

**벌어진 일.** 액션바의 방향키는 키보드 자신의 래치와는 조합됐지만 물리 키보드에서 누르고 있는 Ctrl
과는 조합되지 않았다. 문자열 칸은 아무것과도 조합되지 않아, Ctrl 아래의 `c` 칸이 `c` 를 쳤다.
방향키는 누르고 있어도 반복되지 않았다. 반복을 넣자, 누르기 전에 켠 한 번짜리 Ctrl 이 첫 걸음에만
조합되고 소진되어 나머지는 한 글자씩 움직였다(0.1.174). 키패드 페이지에는 반대 구멍이 있었다. Shift
가 아예 없어 거기서는 방향키로 선택할 수 없었다(0.1.173).

**고친 방법.** 출처는 셋, 합집합은 하나다. 액션바의 수정자 칸, 키보드의 래치, 물리 키보드에서 눌린
키(`HeldHardwareModifiers`, §9). 액션바는 손가락이 닿고 떨어질 때를 알리고(`onPress`), 서비스는 그때
합집합을 읽어 한 번짜리 래치를 한 번 소진한 뒤 그 누름의 모든 이벤트에 그 집합을 쓴다. 글자나 숫자
하나짜리 문자열 칸은 키다. Ctrl/Alt/Meta 아래에서는 그 조합으로 보내고, Shift 만 있으면 대문자로
친다(`BarKeyChord`). 키패드와 커서 패드는 빈 칸에 Shift 를 얻었고, 글자 페이지의 Shift 와 똑같이
동작한다.

**규칙.** 키는 어디에 그려져 있든 키다. 어느 출처에서든 눌려 있을 수 있는 수정자는 그 키에 걸린다.
그리고 누름에 속한 상태는 그 누름이 만드는 이벤트마다가 아니라 누름이 시작될 때 읽는다.

### 15.33 클립보드를 흉내 내는 기록

**벌어진 일.** 키보드의 클립 목록은 키보드 자신의 복사·잘라내기 뒤에만 기록했다. 브라우저에서 복사한
문구는 목록에 나타나지 않았고, 목록에서 고른 항목은 입력되지만 클립보드가 되지는 않아 앱 자신의
붙여넣기가 목록과 어긋났다 — 하나처럼 보이는 두 클립보드(소유자 신고). 그것을 고치자 두 번째 결함이
드러났다. 목록은 패널을 열 때만 저장소에서 읽었으므로, 키보드가 켜진 뒤 처음 기록되는 클립은 빈
목록에 더해져 기록 전체를 덮어쓰며 저장됐다(0.1.174).

**고친 방법.** `onCreate` 에서 `OnPrimaryClipChangedListener` 를 등록해 바뀔 때마다 기록한다. 목록을
열 때 현재 클립을 읽는다. 고른 클립은 입력하기 전에 클립보드에 넣는다. 민감한 칸에서 복사한 것뿐
아니라 복사한 앱이 민감하다고 표시한 클립(`ClipDescription` extras, 안드로이드 13)도 건너뛴다.
저장된 목록은 어느 경로가 기록하든 첫 기록 전에 읽는다.

**규칙.** 시스템 상태의 편의용 사본은 시스템을 양방향으로 따라가야 한다. 그리고 늦게 읽어 들이는
캐시는 첫 읽기 전만이 아니라 첫 쓰기 전에도 읽어 들여야 한다.

### 15.34 음절을 만드는 도중에 도착한 키

**벌어진 일.** 음절이 아직 조합 중일 때 누른 키 이벤트 — 액션바의 방향키, 소프트 Ctrl 조합, 키패드의
Tab 이나 Esc, 원시 엔터 — 는 두 동작의 계획이 됐다. 음절을 확정하고, 그다음 키를 보낸다. 실행기의
일괄 경로는 확정은 할 수 있어도 키 이벤트는 보낼 수 없어 예외를 던졌고, 서비스는 예외를 잡아
조합기를 초기화하고 실패를 알렸다. 음절은 이미 들어갔으므로 사용자에게는 키가 아무 일도 안 한 것으로
보였다. Termux 에서 가 를 치고 엔터를 누르면 가 만 들어가고 명령은 실행되지 않았다. 일반 입력칸과 띠
방식 터미널이 똑같이 걸렸고, 원격 데스크톱과 직접 그리는 터미널만 피했다. 거기서는 음절이 이미
화면에 있어 계획이 키 하나뿐이었기 때문이다. 조합표의 첫 실행이 찾았고 진짜 Termux 가
확인했다(0.1.175).

**고친 방법.** `CheckedEditorExecutor` 가 "쓰기 뒤에 키 이벤트 하나"인 계획을 알아보고, 쓰기를 따로
한 계획으로 실행한 다음 키를 단일 키 경로로 보낸다. 끝에 편집기 동작이 붙은 계획에 이미 해 오던 것과
같은 분할이다.

**규칙.** 키보드를 살려 두려고 잡은 예외는 모두에게서 숨긴 결함이기도 하다. 조합기가 두 종류의
동작을 한 계획에 넣을 수 있는 곳이면 그 계획을 처음부터 끝까지 시험할 것. 그리고 입력 경로에서 잡힌
예외는 시험이 볼 수 있는 실패로 다룰 것.

### 15.35 페이지를 바꿔 버리는 빗나감

**벌어진 일.** 에뮬레이터에서 천지인과 나랏글 문장을 흔들린 지점(키의 4분의 1 표준편차)으로 쳐 보니,
탭 하나를 기점으로 문장 전체가 틀어졌다. 기능 열이 첫 한글 열 바로 옆에 있다. ㅣ 를 노린 탭이
**123** 에, ㄹ 을 노린 탭이 **Move** 에 떨어졌고, 그 뒤의 모든 키가 숫자를 치거나 커서를 옮겼다.
스페이스를 노린 탭은 바로 위 ⌫ 에 떨어져 글자를 지웠다. 이와 별개로 천지인에서는 손끝이 14~20 dp
미끄러진 탭 — 빠르게 칠 때 흔한 — 이 끌어서 넣은 글자로 나왔다. 14 dp 끌기가 이미 끌기로 판정됐기
때문이다(0.1.176).

**고친 방법.** `TouchTargets` 가 모든 터치의 키를 정한다. 비싼 키 — 페이지나 오버레이를 바꾸는
키(123, Move, 漢, 층·자판 키)나 지우는 키(⌫) — 는 평범한 키와 맞닿은 쪽의 칸 일부, `YIELD` = 폭이나
높이의 35 % 를 그 키에 내준다. 비싼 키를 누르려면 마음먹고 눌러야 하고, 모든 픽셀은 여전히 어느 키에
속한다. 끌기 거리는 14 dp 에서 18 dp 로 늘렸고, 일부러 22·30 dp 끈 입력은 여전히 끌기다. 같은 시드로
전후를 재면 흔들림 25 % 에서 천지인 16 → 8, 나랏글 14 → 8 글자로 줄었고 빗나간 탭 뒤로 문장이
사라지는 일은 없어졌다. 20 dp 까지 미끄러지는 탭은 천지인 21 → 6.

**규칙.** 빗나감은 선이 그어진 자리가 아니라 치르는 값으로 따져라. 잘못 누르면 글자 하나보다 큰
손해를 내는 키는 주변 키보다 실수로 누르기 어려워야 한다.

### 15.36 키보드보다 오래 사는 패널

**벌어진 일.** 노트와 클립 목록은 키보드를 내려도 열린 채로 남았고, 다음에 어떤 칸이 키보드를 부르면
키보드 자리에 다시 나타났다. Termux 에서는 반복이 됐다(이슈 #8). 노트가 IME 창을 화면 전체로 키우면
Termux 가 키보드를 숨겨 달라고 요청하고(입력기 기록에 `TermuxActivity` 의 `HIDE_SOFT_INPUT`), 다음
입력이 노트를 다시 불러와 또 숨겨졌다.

**고친 방법.** `onFinishInputView` 와 `onWindowHidden` 이 두 패널을 닫고, 노트는 평소처럼 저장한다.
Termux 에서 Memo 는 이제 키보드와 함께 닫힌다. 거기서 노트를 쓰게 하려면 터미널 크기를 바꾸지 않고
위에 겹쳐 떠야 한다.

**규칙.** 키보드를 대신하는 패널은 키보드가 화면에 있는 동안만 산다.

### 15.37 손가락이 떠나야 듣는 수정자

**벌어진 일.** Shift 와 Ctrl/Alt/Meta 래치는 손가락이 떨어질 때 적용됐다. 빠르게 치면 키가 서로 굴러
겹친다. Shift 를 누르고, 글자를 누르고, 글자를 먼저 뗀다. 그러면 글자는 Shift 없이 입력되고, 뒤늦게
떨어진 Shift 가 다음 키에 걸린다. 빠가다 가 ㅂ까다 로 나왔다 — 한 동작에 오타 둘. 시각을 정확히
지정해 재 보니(에뮬레이터, 2026-09-16) "Shift 를 먼저 뗀" 경우를 빼고 모두 틀렸다. 물리 키보드처럼
Shift 를 누른 채 글자를 톡 치는 방식도 틀렸다.

**고친 방법.** 수정자 키는 **누르는 순간** 걸린다. 그 손가락이 눌려 있는 동안에는 한 번짜리 상태를
아무도 소비하지 않으므로 그 아래에서 친 글자마다 적용되고, 손가락이 떨어질 때 그동안 무언가
입력됐으면 한 번짜리를 소비하고, 아무것도 입력되지 않았으면 다음 키를 위해 남긴다. 길게 누르면
여전히 고정된다.

**규칙.** 다른 키의 뜻을 바꾸는 키는 눌리는 순간 효력이 있어야 한다. 그러지 않으면 두 손가락이
겹치는 순간 깨지고, 빠른 입력이란 곧 겹침이다.

### 15.38 오타가 실제로 있던 자리

**벌어진 일.** 흔들린 지점으로 자동 생성한 탭을 키보드가 무엇을 쳤는지와 대조해 두 가지를
쟀다(에뮬레이터, 2026-09-16).

- **액션바가 키 높이를 깎고 있었다.** 붙인 상태에서 바와 키가 한 높이를 나눠 써서, 바를 켜면 모든
  줄이 30 % 낮아졌다. 같은 손가락 흔들림에서 빗나감이 0.7 % → 3.7 %(σ 5 dp), 3.0 % → 7.3 %(σ 7 dp)
  로 늘었다. v0.1.177 은 그래서 바를 키 **위에 더했고**, v0.1.179 에서 사용자가 다시 높이 **안쪽**으로
  돌리기를 청했다 — 원래 있던 자리다. 바를 높이 계산에 넣을지는 따로 규칙을 둘 만큼 중요한 문제가
  아니고, 줄이 낮게 느껴지면 높이 설정을 올리면 된다. 측정은 여전히 참이며, 그래서 그 해법을 여기
  적어 둔다.
- **시스템의 길게 누르기가 보조 문자를 치고 있었다.** 400 ms 기준에서 380~450 ms 눌린 키 — 서두르지
  않는 입력에서 흔한 — 의 23 % 가 글자 대신 보조 문자였다. 보조 문자를 가진 키는 이제
  `ALTERNATE_HOLD_MS`(520 ms, 시스템 값이 더 길면 그쪽)를 기다린다. Shift 와 수정자는 시스템 값을
  그대로 쓴다. 거기서는 길게 눌러도 글자가 입력되지 않기 때문이다.

**그리고 빗나감을 가장 싸게 고칠 수 있는 자리**: 자음 키와 모음 키 사이 선에서 `JAMO_BAND` 안에
떨어진 터치는 음절이 다음에 받을 수 있는 것으로 정한다. 조합 중이 아니면 자음, 자음 하나만 있으면
모음, 모음까지 들어간 음절이면 아무것도 가정하지 않는다(`JamoExpectation`). 수정자 키는 맞닿은 글자
키에 가장자리의 5 분의 1 을 내준다. 페이지를 바꾸는 키가 3 분의 1 을 내주는 것과 같다(§15.35). 키당
90 ms 로 친 문장은 77 자 중 오타 2 개에서 0 개가 됐다. 70 ms 에 키의 4 분의 1 흔들림에서 남은 것들은
자음↔자음, 모음↔모음이라 철자로는 가를 수 없다.

**규칙.** 무엇을 고칠지 고르기 전에 오류가 어디 있는지 재고, 고친 뒤에 다시 재라. 이 넷 중 셋은
그동안 짐작으로 꼽아 온 어떤 것보다 값이 컸다.

### 15.39 편집기가 만든 적 없는 선택

**벌어진 일.** 액션바나 키보드의 Shift+화살표가 평범한 글자 칸에서 선택을 만들지 못하고 커서만
옮겼다. `TextView` 는 화살표가 선택을 늘릴지를 **Shift 키가 눌렸는지**(버퍼의 `MetaKeyKeyListener`
상태)로 판단하지, 화살표 이벤트에 실린 메타 상태로 판단하지 않는다. 그런데 키보드는 키 이벤트로
지우는 편집기에만 수정자 누름을 앞세워 보내고 있었다.

**고친 방법.** Shift 의 누름은 모든 편집기에 앞세워 보낸다. 나머지 수정자는 지금처럼 메타 상태로만
보낸다. 글자 칸은 그것만으로 자기 단축키를 만든다.

**규칙.** 올바른 메타 상태를 실어 보내는 것과 그 키를 누르는 것은 다르다. 플랫폼이 수정자의 *상태*
를 읽는 곳에서는 수정자를 키로 눌러라.

### 15.40 표 하나, 자판 둘

**벌어진 일.** 발음기호 자판(이슈 #11)을 두 번 적었다. 한 번은 화면에 그리는 줄로, 한 번은 미국식
키 자리를 같은 기호에 잇는 물리 자판 표로. 두 번째 사본에서 키 이름을 한 면 내내 `hardware.key.q`
가 아니라 `hardware.q` 로 적었는데, 아무도 그렇다고 말해 주지 않았다. 아무것도 잇지 못하는 물리
자판 표는 그냥 아무것도 치지 않는다.

**고친 방법.** 표 하나(`IpaKeys`)가 기호와 길게 누르기 대안, 그리고 각 키의 미국식 자리를 함께
갖는다. `KeyboardLayouts` 가 그것으로 그리고 `HardwareLayoutTables` 가 그것에서 표를 만들어 내므로
둘이 어긋날 수 없다. 시험이 자리마다 화면과 전선이 같은지 따라간다.

**규칙.** 같은 배치가 두 곳에 필요하면 한 번 적고 나머지를 유도하라. 베껴 쓴 표는 요란하게 실패하지
않고, 아무도 보지 않는 사본 쪽에서 조용히 실패한다.

### 15.41 보내라고 준 것을 자기가 먹은 판

**벌어진 일.** 메모장의 새 **Send** 링크는 메모를 앱에 쳐 넣어 달라고 서비스에 건넨다. 그대로
보냈더니 메모 안으로 다시 들어갔다. 메모장이 떠 있는 동안 서비스는 모든 키를 메모로
돌리기 때문이다(`consumeForNotepad`).

**고친 방법.** 호스트가 판을 먼저 내리고 나서 보낸다. 글은 붙여넣기가 아니라 친 입력
(`SemanticInput.text`)으로 나가므로 터미널과 원격 데스크톱도 여느 키처럼 받는다.

**규칙.** 입력을 가로채는 판은 그 판을 통해 보내는 것도 가로챈다. 판 너머로 말하려면 판을 먼저
내려라.

### 15.42 쳐다볼 때만 말하는 클립보드

**벌어진 일.** 다른 앱에서 복사한 글이 키보드의 클립 목록에 없는 일이 잦았다. 목록과 안드로이드
클립보드가 따로 노는 느낌이라는 신고가 두 번 왔다. 키보드는 `onCreate` 에서 한 번 등록한
`addPrimaryClipChangedListener` 하나에 기대고 있었다. 안드로이드는 화면에 있거나 현재 키보드인 앱에만
클립보드를 읽게 해 주고, 어떤 ROM 들은 한 걸음 더 나아가 화면에 없는 키보드에는 변경 사실조차 전하지
않는다. 그래서 브라우저에서 한 복사는 아무에게도 닿지 않았고, 키보드가 올라올 때쯤엔 이미 지나간
일이었다.

**고친 방법.** 키보드가 뜰 때마다 스스로 읽는다. `onStartInputView` 와 `onWindowShown` 이
`catchUpWithTheSystemClipboard()` 를 부르고, 이 메서드는 감시를 다시 등록한 뒤(같은 콜백을 두 번
등록해도 등록은 하나이므로 값이 싸다) 클립보드를 읽는다. 화면에 뜬 순간은 읽을 권한이 있는 순간이고,
누군가 다른 곳에서 복사해 이 칸으로 온 바로 그 순간이기도 하다. 사용자가 목록에서 지운 항목은
`forgottenClip` 에 기억해 두어, 아직 클립보드에 남아 있어도 읽기가 그것을 도로 끌어오지 않게 했다.

**어떻게 증명했나.** 에뮬레이터의 AOSP 이미지는 변경을 성실히 전해 주므로, 이걸 덮는다던 칸은 고치기
전에도 통과했다. 게다가 옛 칸의 "바깥 복사"는 키보드 자기 프로세스에서 클립보드를 설정한 것이라 바깥이
아니었다. 도구 두 개가 이것을 진짜로 만들었다. 브라우저처럼 복사하는 별도 패키지
(`testhost/ClipSetterActivity`)와, ROM 이 하듯 리스너를 떼어 내는 탐침(`CLIPWATCH:off`)이다. 감시를 끈
채로, 옛 빌드는 계속 예전 클립을 보여 주었고 새 빌드는 키보드가 돌아오는 즉시 바깥 복사를 갖고 있었다.

**규칙.** 가끔만 주어지는 권한은 *가졌을 때 쓰라는* 권한이다. 플랫폼이 전해 주지 않을 수도 있는 콜백
위에 집을 짓지 말고, 읽어도 되는 순간에 읽어라.

### 15.43 자꾸 아래 키가 이겼다

**벌어진 일.** 12키 자판에서 빠르게 치면 ⌫ 가 공백을 치고 스페이스가 엔터를 보냈다(소유자 신고,
2026-09-16). 한 열에서 원인 둘이 만났다. 첫째, ⌫ 는 값비싼 키라서 자기 칸 아래쪽 35 % 를 아래 키에
내주고 있었는데(§15.38), 그 페이지에서 아래 키는 글자가 아니라 스페이스였다. 보호한다는 것이 정작
백스페이스를 급할 때 누를 수 없게 만들었다. 둘째, 손가락이 시스템의 **터치 슬롭**만큼만 칸을 벗어나도
누름이 옆 키의 것이 되었는데, 빠른 탭은 닿고 떼는 사이에 그만큼 구른다.

**고친 방법.** 값비싼 키는 자기 가장자리를 *글자* 키에만 내준다. 스페이스와 엔터에는 내주지 않는다.
키가 바뀌는 거리는 이제 자기 숫자를 갖는다(`RETARGET_DP`, 가장자리에서 16 dp). 누름이 탭이기를
그만두는 거리와 키가 바뀌는 거리는 같은 질문이 아니기 때문이다. 그리고 잘못 눌렸을 때 가장 비싼 키인
엔터는(메시지를 보내거나 명령을 실행한다) 자기 칸 위쪽 5분의 1을 위의 스페이스에 돌려준다.

**어떻게 증명했나.** 눌렀다가 구르는 탐침(`TAPDRIFT`)으로 신고된 두 경우를 쟀다. 고치기 전에는 낮게
누른 ⌫ 도, 10 dp 구른 ⌫ 도 공백을 쳤고 아무것도 지우지 못했다. 고친 뒤에는 둘 다 지운다. 세 경우
모두 칸으로 남겼다.

**규칙.** 키를 줄이는 보호는 그 키를 누르지 못하게 하는 보호다. 무엇이 비싼지만 묻지 말고 사용자가
무엇을 겨눴는지를 물어라.

### 15.44 셸이 키 입력으로 읽는 붙여넣기

**벌어진 일.** Termux 와 Termius 에서 붙여넣기가 아무것도 하지 않았다. 글자가 아니라 키를 받는
편집기에서는 키보드가 **Ctrl+V** 를 보냈는데, 원격 데스크톱에서는 맞는 선택이지만(저쪽 운영체제가
붙여넣는다) 셸은 Ctrl+V 를 *quoted-insert* 로 읽는다. 다음 글자를 기다렸다가 그대로 집어넣는다.

**고친 방법.** 터미널에서는 안드로이드 클립보드를 읽어 **쳐서 넣는다**. 클립 목록이 이미 쓰던 길이다.
원격 데스크톱은 조합키를 그대로 쓴다. 둘은 이름이 아니라 능력으로 가른다. 터미널은 키를 보내고
게다가 주변 글자가 없으며(`EditorCapabilities.isTerminal`), 원격 데스크톱은 키를 보내지만 더미 버퍼를
갖는다.

**규칙.** 조합키는 저쪽이 그렇다고 여기는 뜻을 갖는다. 그걸 알 수 없는 곳에서는 물건 자체를 보내라.

**그리고 이 규칙의 짝.** 이 수정을 보고 사용자가 선을 그었다. *액션바의 편집 키는 동작이고, 키보드의
Ctrl 은 키다.* 그래서 액션바의 Paste 는 클립보드를 넣고(편집기가 있으면 `performContextMenuAction`,
전선뿐이면 쳐서), Copy·Cut·Select all 은 Ctrl+C 로 나가는 대신 터미널에 선택이 없다고 말한다. 그 키는
복사가 아니라 실행 중인 명령의 중단이다(실측: 고치기 전에는 액션바의 Copy 가 Termux 의 `sleep` 을
죽였다). 소프트 Ctrl 과 글자는 지금처럼 보이는 그대로의 조합키로 나간다. 원격 데스크톱만 양쪽 모두
예외다. 그 연결 뒤에는 동작을 걸 것이 없으므로 액션바도 조합키를 쓴다.

**그래도 이름을 묻는 한 자리(이슈 #9, 다시 열림).** 능력 검사는 커서를 보고하는 터미널을 놓친다. SSH
클라이언트는 화면으로는 터미널이면서 연결에게는 평범한 편집기일 수 있고, Termius 가 바로 그렇다. 그런
앱에는 `performContextMenuAction(paste)` 가 전달되었고, 그런 화면은 그걸로 아무 일도 하지 않는다.
감지할 방법도 없다. 편집기가 일을 했든 안 했든 호출은 참을 돌려주기 때문이다. 그래서 **붙여넣기만**
이름으로 아는 터미널인지도 묻고, 그렇다면 클립보드를 쳐서 넣는다. 나머지 답은 모두 여전히 편집기의
모양에서 나온다. 그래야 터미널 앱 안의 평범한 글자 칸이 평범한 글자 칸으로 동작한다.

### 15.45 키보드가 등록하지 못하게 막던 단축키

**벌어진 일.** 한/영을 Shift+Space 로 등록하면 왼쪽 Shift 하나만 저장되었다. 설정 화면이 Space 를
보기 전에 두 가지가 그것을 먹었다. 캡처를 `onKeyDown` 에서 했는데, 이것은 키가 액티비티를 지나는
*마지막* 자리다. 포커스를 가진 뷰가 먼저 본다. 방금 누른 Add 버튼이 포커스를 갖고 있었고, 버튼은
Space 와 Enter 로 자기를 누른다. 게다가 Shift+Space 가 기본 바인딩이 된 뒤로는 키보드 자신이 그것을
가져갔다. 바인딩된 키는 자기 일을 하는데, 그것이야말로 그 키를 달라고 묻는 화면이 막아야 하는 일이다.

**고친 방법.** 캡처는 뷰보다 먼저인 `dispatchKeyEvent` 에서 한다. 그리고 화면이 기다리는 동안
공유 설정에 `hw_capture_in_progress` 를 세워 두고, 서비스는 물리 키마다 그것을 읽어 전부 통과시킨다.
플래그는 캡처가 끝날 때와 `onPause` 에서 지워지므로, 캡처 중에 화면을 떠나도 키보드가 먹통이 되지
않는다.

**그리고 기본값.** 한/영은 Shift+Space, `KEYCODE_KANA`(218), `KEYCODE_LANGUAGE_SWITCH`(204)에,
한자는 `KEYCODE_EISU`(212)에, 유니코드 입력은 Ctrl+Shift+U 에 묶인 채로 시작한다. 이상해 보이는
코드는 안드로이드의 것이다. 안드로이드의 일반 키레이아웃이 리눅스의 HANGEUL·HANJA 키를 일본어
KANA·EISU 코드에 얹어 두었고, 한국어 키보드의 그 두 키는 그렇게 도착한다. 기본값은 *설정된 적 없는*
항목을 채울 뿐이므로, 일부러 비운 목록은 비운 대로 남는다.

**규칙.** "원하는 키를 누르세요"라고 묻는 화면은 묻는 동안 모든 키의 주인이다. 뷰보다 먼저 이벤트를
받고, 앱의 나머지에게는 물러나 있으라고 말하라.

### 15.46 복사한 적 없는 글

**요청.** 키보드는 클립보드를 읽는다. 어떤 ROM 에서는 백그라운드에 있는 키보드가 읽어 끌 수 없는
기록에 남긴다. 이슈 #10 의 요청은 더 나은 클립보드가 아니라 클립보드를 **우회하는 길**이었다. 앱이
키보드로 글을 **공유**하면 키보드 자기 저장소에 넣어 두었다가, 부르면 쳐서 넣어 달라. 클립보드에는
아무 말도 하지 말고.

**만든 것.** 공유 수신처(`ShareTargetActivity`, `ACTION_SEND`·`ACTION_PROCESS_TEXT`, 권한 없음,
화면 없음)가 자기 설정 파일(`StashStore`)에 적는다. 클립 판에는 클립보드 목록 위로 둘째 목록이
생겼고, 그리는 방식도 동작도 다르다. 클립을 고르면 여전히 클립보드에도 올라가지만, 보관 항목을 고르면
**치기만** 한다. 규칙은 `StashHistory` 가 쥔다 — 새것이 앞, 최대 스무 개, 사용자가 정한 시간(10분·
1시간·1일·지울 때까지)에 사라짐, 같은 글은 중복 대신 앞으로 이동. 안드로이드에 기대지 않으므로 규칙이
곧 유닛 시험이다.

**짝.** v0.1.180 에서 우리는 키보드가 뜰 때마다 클립보드를 읽게 만들었는데, 이 요청을 하는 사용자에게는
정반대의 동작이다. 그래서 *시스템 클립보드 따라가기* 를 설정으로 두었다(기본 켬). 끄면 키보드는
클립보드를 전혀 읽지 않고, 판은 빈 목록 대신 그 사실을 적고, 공유만이 들어오는 길이 된다.

**규칙.** 플랫폼의 통로 자체가 문제라면 그 통로를 더 안전하게 만들려 하지 말고, **다른 통로**를 하나
내주고 사용자가 고르게 하라.

### 15.47 형식은 약속이므로 되도록 적게 말하라

**요청.** 릴리즈를 기다리지 않고 자판을 추가하는 길. 앱에 공유하는 파일(이슈 #11). 신고자는 JSON 이든
자체 형식이든 좋다고 했고, 다른 키보드의 배열을 베끼는 저작권 문제를 걱정했다.

**만든 것.** 머리글 한 줄, 이름, 세 글자 약칭, 그리고 `row:` 세 줄로 된 줄 형식. 각 칸은 "그 키가 치는
글자"이고 막대 뒤는 길게 누르면 나올 글자다. 그 외에는 없다. Shift·백스페이스·엔터·아래 줄·자판 키는
키보드 것이다. 설치는 한 번에 하나, 원문 그대로 자기 설정 파일에 두고 `UserLayout` 으로 읽는다.
안드로이드에 기대지 않으므로 형식이 곧 유닛 시험이다. 들어오는 문은 보관함이 이미 낸 공유 문이다.
머리글로 시작하면 자판이고, 아니면 보관할 글이다.

**살아남게 만드는 규칙.** *모르는 줄은 거절하지 않고 무시한다.* 나중 버전용으로 쓴 파일이 지금 버전에서
할 수 있는 만큼은 동작한다. 이 규칙 하나가 약속을 깨지 않고 형식을 키우게 해 주며, 자기 시험을 갖는다.

**그래도 면이 강제하는 것.** 파일이 뭐라 하든 격자는 열 칸이다. 짧은 줄은 채우고 긴 줄은 자른다. 기기
첫 실행이 그 이유를 보여 주었다. 키 세 개짜리 줄이 키보드 자신의 그리기 안쪽
`every row must span exactly 10 columns` 을 던졌다. 남이 쓴 파일 때문에.

**규칙.** 요청에 답하는 가장 작은 형식을 공개하라. 형식에 들어간 것은 영원히 읽어 주겠다는 약속이고,
빼 둔 것은 나중에 해도 되는 결정이다.

### 15.48 편집기가 틀릴 수 있는 세 번의 호출

**벌어진 일.** 어떤 앱에서 나랏글로 신재님을 치면 ㅈ 을 만드는 순간 신 이 사라졌다(소유자 신고,
2026-09-17). ㅈ 은 ㅅ 에 획을 더한 것이고, 획은 그 ㅅ 이 닫은 음절을 되돌려 달라고 한다. 조합기는 세
번의 호출로 답했다. 조합을 비우고, 한 글자를 지우고, 둘을 다시 조합한다. 연달은 세 번의 호출은 커서와
조합 구간이 어디인지에 대해 편집기가 틀릴 기회가 세 번이라는 뜻이고, 틀린 편집기는 조합 앞의 글자를
지운 뒤 되돌려 놓지 않는다. 같은 빌드로 평범한 `EditText` 에서 같은 낱말을 치면 제대로 나왔다(에뮬레이터).
철자가 아니라 모양이 잘못이었다는 증거다.

**고친 방법.** `RECOMPOSE_PREVIOUS` — 커서 뒤의 글자들을(조합 중인 것 포함) 조합 구간으로 되잡아 새
조합으로 만든다. `setComposingRegion` 다음 `setComposingText`, 삭제는 없다. 잃을 것이 없다. 구간을
잡아 주지 않는 편집기는 거절로 그렇게 말하고, 그런 편집기에는 옛 세 호출이 돈다. 애초에 조합을 보지
않는 편집기(터미널의 띠, 원격 데스크톱의 커밋)는 이미 이해하는 모양 그대로 둔다.

**규칙.** 플랫폼에 그 뜻을 담은 호출이 하나 있으면 세 개로 풀어 쓰지 마라. 호출 하나가 늘 때마다
편집기가 당신과 다르게 생각할 자리가 하나 늘어난다.

### 15.49 한 면에 알파벳을 담을 수 없다면, 물어볼 수 있게 하라

**요청이 정말로 말한 것.** 발음기호 자판은 "IPA 기호를 갖고 싶다"에 답했지만(이슈 #11), 신고자는 다른
키보드의 발음기호 면을 왜 안 썼는지도 함께 적어 두었다. "기호를 찾기 어렵다". 그래서 저장해 둔 문구를
썼다 — ɑ 를 얻으려고 "palm". 그건 검색이고, 키 스물일곱 개짜리 면이 기호 백육십 개의 답이 될 수는
없었다.

**만든 것.** 면 위의 키 하나, 문 두 개. **누르면** 치는 것이 질의가 된다. X-SAMPA 코드(`T` 는 θ 이고
대문자가 뜻을 가른다 — `t` 는 t), 기호의 이름(schwa·nasal·fricative), 그리고 영어 모음은 Wells 의
어휘집합(PALM·THOUGHT·KIT). 사전을 읽는 사람이 이미 가진 어휘다. **길게 누르면** 분류가 나온다 —
모음·파열음·마찰음… — 하나를 고르면 그 분류의 기호들이 나온다. 무엇에든 두 번이면 닿고, 외울 것은
없다. 표의 낱말이 곧 지도다.

**어디에 그리나.** 한자 목록이 쓰는 후보 패널을 그대로 쓰되, 문서 위에 띄우지 않고 **키보드 위에**
얹는다. 검색은 치는 것이므로 키가 손에 닿아 있어야 한다. 한자 목록은 계속 띄운다. 거기엔 칠 것이 없다.

**기기 첫 실행이 가르쳐 준 두 가지.** 입력 뷰를 만들면 키보드 뷰가 **새로 생긴다**. 패널을 열기 전에
걸어 둔 Shift 잠금은 열고 나면 없다 — 면을 뒤집는 것은 그 다음이다. 그리고 TCG 에서는 그 재구성이
느려서, 바로 뒤에 두드리는 탐침은 옛 뷰를 친다. 손으로 같은 순서를 하면 된다. 재구성과 경주하는 시험은
에뮬레이터를 재고 있는 것이다.

**규칙.** 한 면이 그 영역을 담을 수 없으면, 키보드가 할 일은 더 큰 면이 아니라 **사용자가 이미 말할 수
있는 질문**이다.

### 15.50 한 색에 세 가지

**벌어진 일.** 눌린 키는 강조색으로 칠한다. 길게 눌러 뜨는 대안 띠도, 그 위의 미리보기 상자도 같은
색이었다. 손가락이 눌려 있고 띠가 떠 있는데, 화면 어디에도 "지금 고르는 중인 것"이 무엇인지 말해 주는
것이 없었다(소유자 요청, 2026-09-18).

**고친 방법.** 고르기에 자기 색을 주었다. 강조색을 색상환에서 210° 돌린 색이다(`ChoiceHue`). 테마의
채도와 밝기를 그대로 두므로 밝은 테마에서도 어두운 테마에서도 읽히고, 기기가 Material You 팔레트를
쓰면 그 팔레트를 따라간다. 띠에서 겨눈 칸은 그 색으로, 나머지는 그 색으로 물들여 칠하고, **골라서**
넣은 글자(길게 누르기·끌기·띠에서 고르기)는 그 색으로 메아리친다. 눌러서 넣은 글자는 여전히 강조색이다.

**그리고 상자는 설정이 되었다.** 상자는 키 위에 그린다. 그것이 요점이고(손가락이 누른 키를 가린다)
동시에 반론이다(다음에 누를 키 바로 위에 놓일 수 있다). 그래서 켜고 끄기, 그리고 투명도. 낮추면 키가
비친다. 다만 글자의 잉크는 상자가 아무리 흐려도 `EchoBoxSettings.MIN_INK_OPACITY` 아래로는 안 내려간다.
35 % 에서 둘이 함께 흐려져 글자도 키도 못 읽었다(에뮬레이터 실측).

**소유자가 보고 바꾼 것.** 상자는 꽉 찬 색이 아니라 60 % 로 시작한다. 상자는 키 위에 있고, 키가 상자보다
중요하다. 그리고 앱 첫 화면의 설정 항목 두 개 앞에 있던 ⚙ 를 뺐다. 이름이 이미 무엇인지 말하는데 그
앞에 붙은 기호는 장식이다.

**규칙.** 색은 이름이다. 서로 다른 둘이 한 색이면 그중 하나는 이름이 없는 것이다.

### 15.51 흰 키의 자판

**벌어진 일.** 밝은 테마가 키 면을 있는 것 중 가장 밝은 중립색으로 칠했다. 손으로 고른 쪽은 거의 흰색,
Material You 에서는 `system_neutral1_50`. 소유자는 그 결과를 키 묶음이 아니라 종이 한 장으로 읽었다
(2026-09-18).

**고친 방법.** 양쪽 다 사다리에서 한 칸 내렸다. 손으로 고른 면은 238/240/244, 바탕은 198/202/209.
동적 팔레트는 `system_neutral1_100` 위에 `system_neutral1_200`. 사다리 자체는 그대로다 — 면이 바탕
위, 비활성이 아래 — 그래서 여기서 유도되는 다른 색들(눌림 틴트, 고르기 색, 잉크 대비)이 함께 움직였고
두 번째 조정이 필요 없었다.

**규칙.** 테마 색은 목록이 아니라 사다리다. 색 하나가 아니라 칸 하나를 옮겨라.

### 15.52 화면 전체를 달라고 하던 패널

**벌어진 일.** 터미널과 채팅 앱에서 Memo 와 Clip 이 열리자마자 닫혔고, 열리는 앱에서는 패널의 윗부분이
상태 표시줄 *아래*에 깔렸으며 아래 앱이 키보드가 화면을 다 채운 것처럼 자기 배치를 다시 잡았다. 제보자의
화면 사진이 두 가지를 모두 보여 줬다(이슈 #8).

**왜.** 패널을 화면 높이로 재고 있었다. IME 창은 화면 아래에 붙으므로 그만큼 높은 창은 시스템이 그리는
위쪽 띠 뒤까지 올라간다. 그것이 사진이다. 그리고 자기 영역이 입력 창에 눌려 없어진 앱은 입력기가 방해가
된다고 판단해 내려 달라고 요청할 수 있다. 키보드가 내려가면 패널도 함께 내려가고, 다음 키 입력이 둘을 다시
불러온다. 그것이 깜빡임이다.

**고친 방법.** 입력기가 실제로 가진 공간에 대고 잰다. 화면에서 위쪽 띠를 빼고, 키보드가 아래에 이미 비워
둔 만큼을 빼고, 산수가 무엇이라 하든 화면의 5분의 4 를 넘기지 않는다. 5분의 1 을 앱에게 남기는 것이, 앱이
"남은 자리가 없다"고 결론 내리지 않게 하는 장치다. 계산은 단위 시험이 붙은 자기 클래스에 있고, 기기 칸
두 개 — 진짜 Termux 안에서 노트와 클립 목록을 열고 4초 뒤에도 열려 있는지 — 가 그것을 지킨다.

**규칙.** 규칙을 내가 정하지 않은 창은 캔버스가 아니다. 시스템의 가구를 뺀 나머지가 얼마인지 묻고, 앱이 제
발로 서 있을 만큼 남기고, 그 산수를 시험이 닿는 곳에 두어라.

### 15.53 잘 된 것만 놓아 주던 수식키 묶음

**벌어진 일.** 2026-09-17 독립 검토(발견 R01)가 원격 데스크톱의 수식키 묶음을, 시키는 대로 실패하는 가짜
편집기에 흘려 보았다. Ctrl 을 누른 직후 세션이 끝나면 실행기는 놓아 주는 고리에 닿기 전에 "보내지 않음"을
돌려주었고 Ctrl 은 끝내 올라오지 않았다. 편집기가 Ctrl 을 거절하면 고리는 수식키를 더 누르지 않았을
뿐 C 는 그대로 보냈다. 그래서 Ctrl+C 가 맨 `c` 로 도착했고, 결과는 단축키가 잘 된 것으로 보고됐다.

**왜.** 프레임(§15a)은 모든 호출이 성공하는 길을 위해 쓰였다. 실패 출구는 이른 반환이었고, 놓아 주는
고리는 그 뒤에 있었으며, 실제로 누른 것이 아니라 프레임의 수식키 전부를 놓았다. 수식키 누름도 저쪽에
보내는 쓰기인데, 본 키가 받던 세션 확인을 받지 않았다.

**고친 방법.** 묶음은 자기가 누른 것을 책임진다. 수식키는 세션이 아직 이 세션일 때만 누르고, 거절·예외·
낡은 세션 뒤에는 더 누르지 않으며 본 키도 보내지 않는다. 누른 수식키는 전부 `finally` 에서 역순으로,
그 끝점 자신의 브리지 — 눌렀던 바로 그 연결, 그 자리를 이어받은 편집기가 아니다 — 로 놓고, 하나를 놓다
실패해도 다음 것을 건너뛰지 않는다. 결과는 프레임의 호출까지 세고, 깨끗한 네 호출이 아니면 "보냄" 이
아니라 "불확실" 이다. 그래서 서비스가 그 이벤트를 앱에 넘기지 않는다. 키 바로 앞에 쓴 음절(비우고-키
계획)은 키가 실패해도 쓴 만큼이 결과에 남는다.

**규칙.** 한 순서가 누른 것은 같은 순서가, 같은 연결에서, `finally` 안에서 놓는다. 현장에서 우연히
실패한 호출 하나가 아니라, 모든 호출이 하나씩 실패하는 경우를 시험하라.

### 15.54 지우기가 실패한 뒤에도 커밋하던 교체

**벌어진 일.** 가나의 ゛゜小 키와 한자 고르기는 둘 다 커서 앞 글자를 바꾼다. 지우고, 커밋한다. 둘 다 그
호출을 `InputConnection` 에 곧장 썼다(검토 R02). 가나는 지우기가 됐든 안 됐든 커밋했으므로, 거절된
지우기가 か 를 かが 로 만들었고, 예외는 `endBatchEdit` 를 건너뛰었다. 한자 고르기는 후보를 띄운 뒤 커서가
움직였거나 필드가 바뀌었어도, 누르는 순간 커서 앞에 있던 것을 바꿨다.

**고친 방법.** `TextReplacement` 가 둘 다 맡는다. 시작, 지우기, 커밋, 그리고 언제나 끝내기. 지우기는
전제 조건이라, 거절되거나 예외를 낸 지우기 뒤에는 아무것도 커밋하지 않는다. 한자 고르기는 먼저 후보를
띄웠던 그 읽기가 아직 그 자리에 있는지 본다. 같은 세션 세대, 커서 앞의 같은 글자(또는 같은 선택). 아니면
후보를 닫고 아무것도 쓰지 않는다.

**규칙.** 교체는 순서가 있는 두 편집이다. 두 번째를 보내기 전에 첫 번째를 확인하고, 바꾸려는 것이 아직
바꾸라고 했던 그것인지 확인하라.

### 15.55 예외를 낸 호출 뒤에 있던 정리

**벌어진 일.** `onFinishInput` 은 세션을 멈추기 전에 편집기에 조합을 끝내 달라고 했다. 그 호출이 정리를
담은 `try/finally` 바깥에 있어서, 거기서 편집기가 예외를 내면 `stopAccepting` 도 `finishSession` 도
건너뛰었다(검토 R10). §15.5 의 규칙이 자기 첫 줄에서 깨진 것이다. `onFinishInputView` 도 같은 모양이었다.

**고친 방법.** `finishComposingInEditor` 는 예외를 내지 않고, 두 정리 모두 나머지 일을 그 뒤의 `finally`
에서 한다. 프레임워크 자신의 `onFinishInput` 도 `finishComposingText` 한 번이므로 거기서 나는 예외도 잡는다.

**규칙.** 정리에서 첫 줄은 실패할 수 있는 자리다. 반드시 일어나야 하는 일은 그 뒤 `finally` 에 둔다.

### 15.56 자판 하나에만 귀를 기울이던 찾기

**벌어진 일.** 발음 기호 찾기가 열려 있을 때, 화면 글자는 질의에 붙었는데 물리 글자는 문서에 입력됐다
(검토 R20). `onKeyDown` 은 후보 띠를 숨기고 하던 일을 계속했으므로, 아무것도 보이지 않는 채 찾기가 열려
남았고 — 다음 필드의 첫 낱말이 그 질의로 들어갔다.

**고친 방법.** `IpaKeyRoute` 가 물리 키에도 화면 키와 같은 답을 준다. 글자는 질의에 붙고, 백스페이스는 하나
지우며(빈 찾기는 닫고), Escape 는 닫고, 숫자와 쪽 넘김 키는 후보에게 남기고, 수식키 단독은 통과시키고
(대문자는 Shift 로 치므로), 그 밖의 키는 찾기를 닫고 평소대로 간다. 찾기가 써 버린 키 누름은 그 키 뗌도
써 버린다. 새 필드가 오거나 키보드가 내려가면 찾기는 끝난다.

**규칙.** 입력을 가로채는 모드는 입력하는 모든 길을 가로채고, 자기가 열린 필드의 가장자리에서 끝난다.

### 15.57 실패할 수 없던 관문

**벌어진 일.** 로컬 관문 둘이 확인하지 않은 것을 보고하고 있었다(검토 R14, R15). 계측 실행기는 modern/
legacy flavor 가 생기며 모호해진 Gradle 작업 이름을 여전히 불렀고, 이제 없는 APK 경로를 설치했다 —
예전 빌드의 찌꺼기가 시험 대상이 될 수 있었다. 문장 매트릭스는 한 행을 명령 치환 안에서 셌는데, 그 서브셸이
셈을 버렸으므로 FAIL 을 찍고도 0 으로 끝날 수 있었다. 인터랙션 매트릭스는 키보드를 에뮬레이터 하나만
가진 크기와 비교했다.

**고친 방법.** 실행기는 flavor 를 받고, 빌드 전에 그 변형의 APK 를 지우며, 각 APK 곁에 자기 변형을 가리키는
빌드 메타데이터가 없으면 아무것도 설치하지 않는다. 매트릭스의 모든 행은 부모 셸의 판정 함수 하나를 거치고,
로그 줄을 못 찾은 읽기는 빈 필드가 아니라 자기 값을 가지며, 종료 코드는 0 아니면 1 이다. 크기 행은 바를
켜기 전과 뒤의 키보드를 재어 둘을 비교한다. 각 관문에는 한 행씩 실패시키는 시험이 붙었다.

**규칙.** 관문은 실패하는 것을 본 적이 있어야 증거다. 모든 행을 한 번씩 일부러 깨 보라.

### 15.58 비밀번호 필드를 읽던 도우미들

**벌어진 일.** 비밀번호 필드에서 조합하는 일은 되게 해 두었고(§15.30, 이슈 #7), 실행기는 민감한 필드의 글을
결코 읽지 않는다. 그런데 서비스는 여섯 군데에서 필드를 직접 읽었다. 낱말 선택, 가나 ゛゜小 키, 한자 키,
한자 고르기의 "읽기가 아직 그 자리에 있나" 확인, 이미 쓴 글자에 긋는 나랏글 획, 그리고 조합 중인 글자를 다시
읽어 보는 커서 확인이다. 그중 어느 것도 이 필드가 비밀번호인지 묻지 않았다(검토 R03).

**고친 방법.** 질문 하나, `mayReadEditorText()` 를 그 모두 앞에서 묻는다. 비밀번호 필드에서 그들은 아무것도
하지 않는다. 조합은 계속된다. 조합은 키보드가 스스로 쥔 것만 쓰기 때문이다.

**규칙.** 한 경로에서만 지키는 개인정보 규칙은 그 경로의 규칙일 뿐이다. 한 곳에 두고, 모든 읽기가 그것에
묻게 하라.

### 15.59 비밀번호라고 말하지 않은 클립

**벌어진 일.** 보이는 비밀번호 필드에서 복사하면 그 글이 클립 기록에 남았다(검토 R18, 실제 EditText 에서
재현). 키보드 자신의 복사 키는 이미 민감한 필드의 것을 기억하지 않았다. 그러나 클립보드 리스너 — 어느 앱이
복사했든 모든 복사를 듣는다 — 는 복사한 앱의 표시만 보고 판단했고, 필드 자신의 복사는 아무 표시도 달지 않는다.

**고친 방법.** `ClipRetentionGuard`. 앱이 표시한 클립, 지금 입력 중인 필드가 비공개(어떤 종류든 비밀번호,
또는 개인화 학습을 하지 말라고 한 필드)일 때의 클립, 비공개 필드를 떠난 지 1초가 안 된 때의 클립은 보류한다.
리스너는 초점이 옮겨 간 뒤에야 복사 소식을 들을 수 있기 때문이다. 보류한 클립은 클립보드가 다른 것으로
넘어갈 때까지 계속 보류하므로, 다음에 키보드가 나타나며 따라잡을 때도 줍지 않는다. 쥐고 있는 것은 그 글의
요약값(digest)뿐이다.

**규칙.** 클립보드는 클립이 어디서 왔는지 말해 주지 않는다. 사용자가 있던 필드가 말해 준다.

### 15.60 잠금 화면용 키보드가 일기장을 열었다

**벌어진 일.** 서비스는 direct-boot aware 라서, 사용자가 휴대폰을 처음 잠금 해제하기 전에도 키보드가 있다.
그런데 그 `onCreate` 가 설치된 레이아웃을 보통의 SharedPreferences 에서 열었다. 그것은 자격 증명 암호화
저장소에 있고, 잠금 해제 전에는 쓸 수 없다(검토 R04). 모든 설정과 클립·보관함·노트 기록도 거기 있다.

**고친 방법.** 첫 잠금 해제 전까지 서비스의 `getSharedPreferences` 는 모든 호출자 — 뷰와 저장소도, 모두 이
컨텍스트로 저장소에 닿으므로 — 에게 메모리에만 있는 설정을 건넨다. 기본값이고, 아무것도 읽지 않고, 디스크에
아무것도 쓰지 않는다. 클립보드도 기록하지 않는다. 잠긴 키보드를 풀린 키보드처럼 보이게 하려고 개인적인 것을
기기 보호 저장소로 옮기지 않는다. 잠금이 풀리면 — `ACTION_USER_UNLOCKED`, 또는 풀린 뒤 첫 저장소 읽기 중
먼저 오는 쪽에서, 한 번 — 진짜 설정을 열고 키보드를 다시 짓는다. 그 전에 쓴 것(잠금 화면에서 바꾼 설정)은
일부러 버린다.

이것도 일부러 그렇다. 개인화 학습을 하지 말라고 한 필드(시크릿 탭)에서 한 복사도 기록하지 않고, 비밀번호
변형을 선언한 터미널에서는 §15.58 의 도우미 — 한자, 가나, 낱말 선택 — 가 들어가지 않는다.

**규칙.** direct-boot aware 란, 시작할 때의 모든 저장소 경로가 먼저 "잠금 해제 전인가?" 에 답한다는 뜻이다.
평범한 기본값은 쓸 만한 키보드다. 사용자의 데이터를 그 암호화 밖에 둔 사본은 아니다.

### 15.61 같은 분에 쓴 노트 둘

**벌어진 일.** 노트의 신원이 그 스탬프였고, 스탬프의 단위는 분이다: `20260713-1448`. 1분 안에 쓴 노트
둘은 노트를 찾아보는 모든 곳에 하나의 노트였다. 하나를 체크해 "선택 삭제" 하면 둘 다 지워졌고, 하나를 고치면
다른 쪽에 얹힐 수 있었으며, 화살표는 먼저 있는 것을 움직였다(검토 R05).

**고친 방법.** 노트는 자기 id 를 갖는다. 만들 때 함께 만들어지고 화면에는 나오지 않는다. 열기, 고치기,
선택, 이동, 삭제가 모두 그것을 따른다. 스탬프는 화면에서 늘 그랬던 것 — 날짜 — 로 남는다. 옛 판이 쓴 노트는
저장된 차례에서 id 를 받으며, 같은 저장 텍스트를 읽으면 언제나 같은 id 가 된다.

**규칙.** 시각은 기록에 관한 사실이지 기록의 이름이 아니다. 사용자가 두 번 만들 수 있는 것에는 겹칠 수 없는
신원이 필요하다.

### 15.62 나누려는 것 안에 들어 있던 구분자

**벌어진 일.** 노트·클립 목록·공유 보관함을 기록 사이에 U+001E, 필드 사이에 U+001F 를 넣어 저장했다.
"어떤 자판도 그 글자를 치지 않는다"는 이유였다. 글은 자판에서만 오지 않는다. 붙여넣기나 코드포인트 입력으로
들어온 U+001E 하나가 노트를 둘로 쪼개고 본문을 잘랐으며, 그것을 품은 클립이나 공유 항목은 저장할 때 조용히
사라졌다 — 키보드가 그것을 보여 주고 보관했다고 말한 뒤에(검토 R06, R07).

**고친 방법.** 세 목록의 형식을 하나로 했다. 머리글이 필드 수를 말하고, 각 필드는 길이·콜론·본문으로 쓴다.
필드 안의 무엇도 그 필드를 끝낼 수 없다. 옛 형식은 한 번 더 읽는다. 그 원본은 `<key>_v1` 로 남기고, 새 형식은
그것을 되읽은 결과가 옛 형식에서 읽은 것과 똑같을 때만 쓴다. 새 형식인데 자기 자신으로 되읽히지 않는 글은
`<key>_damaged` 로 남기고 읽힌 만큼만 쓴다.

**규칙.** "아무도 그 글자를 못 친다"는 저장된 글의 성질이 아니다. 무엇이 끝처럼 보이지 않기를 바라는 대신,
필드가 얼마나 긴지 말하라.

### 15.63 어제의 목록을 들고 있던 패널

**벌어진 일.** 클립·보관함 패널은 열릴 때 서비스가 들고 있던 목록으로 그려졌다. 패널이 열려 있는 동안 키보드로
공유된 글은 저장소에는 들어갔지만 그 사본에는 없었고 — 패널에서 항목 하나를 잊게 하면 그 사본이 저장소에
덮여, 방금 공유된 글이 지워졌다(검토 R19, 기기에서 재현).

**고친 방법.** 패널이 하는 모든 변경은 지금 저장된 것에서 시작한다. 불러오고, 그 글을 기준으로 바꾸고,
저장한다. 패널의 사본은 그리기 위한 것일 뿐이다.

**규칙.** 한참 열려 있던 화면이 들고 있는 것은 사진이다. 사진을 그 대상 위에 덮어쓰지 마라.

### 15.64 스스로 설치되던 레이아웃

**벌어진 일.** 레이아웃 문은 들어온 것을 그대로 설치했다. 공유를 걸 수 있는 앱이면 무엇이든 레이아웃을
보내 키보드의 자판을 바꿀 수 있었고, 그에 대한 말은 사후의 토스트 하나뿐이었다(검토 R13). 파서도 그만큼
잘 믿었다. `startsWith("retekey-layout 1")` 이 10판이라고 적힌 파일을 받아들였고, 키 하나가 이십만 자를
치는 것도 쪼개어 저장했다(R09).

**고친 방법.** 문이 묻는다. 이름, 키 캡, 첫 줄의 키, 그리고 무엇을 대신하게 되는지를 Install·Cancel 과
함께 보여 주고, Install 전에는 아무것도 쓰지 않는다. 파일은 주 스레드 밖에서 읽는다 — 얼마나 걸릴지는
남의 제공자가 정한다 — 그리고 한도보다 한 바이트 더 읽어서, 한도를 넘는 파일은 앞부분이 레이아웃처럼
보인다고 읽지 않고 거절한다. 판 번호는 낱말 전체로 보고, 그 앞의 바이트 순서 표시는 허용하며, 파일·줄·키의
크기는 이미 동작하는 레이아웃들에서 끌어낸다. 그들이 필요로 하는 것의 몇 배이고, 넘으면 잘라 맞추지 않고
이유와 함께 거절한다.

**규칙.** 밖으로 열린 문은 지시가 아니라 제안이다. 무엇이 왔는지 보여 주고, 무엇을 대신하게 되는지 말하고,
그 키보드의 주인이 그러라고 할 때까지 아무것도 바꾸지 마라.

### 15.65 우리 쓰기 도중에 온 커서 보고

**벌어진 일.** 원격 데스크톱에 자모통을 치면 가끔 자ㅁㅗ통이 됐다. 한 음절의 자모가 조합되지 않고 하나씩
커밋된 것이다(소유자 보고, 2026-09-23).

**왜.** 다른 기계를 보는 창에는 조합 구역이 없어서 음절을 다시 쓴다. 실현해 둔 것을 지우고, 새 모양을
커밋한다(§15a.1). 그것은 전선 위의 두 조작이고, 클라이언트 자신의 버퍼는 그 각각 뒤에 커서가 어디로 갔는지
보고한다. 그런데 이 키보드는 **계획이 끝나는 자리** 하나만 기억했다(§15a.5). 그래서 지우기의 보고 — 한 칸
뒤의 자리 — 는 어떤 기대와도 맞지 않았고, 사용자가 커서를 옮긴 것으로 읽혀 음절을 그 자리에서 굳혔다. 다음
자모는 되가져올 것이 없어 혼자 커밋됐다. 두 조작이 보통 한 배치에 들어 클라이언트가 그 끝에서 한 번만
보고하기 때문에 가끔씩만 일어났다. 보고가 따로 오면 음절이 깨졌다.

**고친 방법.** 끝이 아니라 모든 걸음을 기대한다. 서비스가 계획의 동작을 하나씩 걸으며 각 동작이 남길 커서를
기록하므로, 지우기의 보고도 다른 것과 같은 기대값이 된다. 우리 자신의 마지막 보고가 남긴 그 자리를 다시
알려 오는 것 — 스스로 반복하는 클라이언트, 아직 따라오지 못한 버퍼 — 도 메아리이지 움직임이 아니다. 우리
쓰기로 설명되지 않는 도약은 여전히 사용자이고, 여전히 음절을 굳힌다.

**규칙.** 키 하나가 여러 조작이 되면 저쪽은 여러 번 답한다. 끝이 아니라 길 전체를 기대하라.

## 15a. 원격 데스크톱 편집기: 뒤에 편집기가 없는 전선

원격 데스크톱 클라이언트(Microsoft Remote Desktop, Chrome Remote Desktop)는 여느 편집기처럼
`InputConnection` 을 내밀지만, 그 뒤에 텍스트 뷰는 없다. 뒤에 있는 것은 릴레이다 — 이쪽에는
숨겨진 더미 버퍼, 저쪽에는 네트워크 건너의 진짜 운영체제. IME 가 편집기에 대해 하는 가정은
이 모양 앞에서 거의 전부, 하나씩, 제각기 다른 방식으로 무너진다. ReteKey 는 이 편집기들을
패키지 이름으로 분류하고, 릴리스 아홉 번(v0.1.132–v0.1.145, 2026-08)에 걸쳐 이 전선이 실제로
받아 주는 것이 무엇인지 배웠다. 배운 순서대로 적는다:

### 15a.1 조합 영역은 없다

`setComposingText` 는 더미를 상대로 "성공"하지만, 저쪽에는 밑줄이 보이지 않고 다음 갱신이 이미
보인 것을 망가뜨린다. **조합을 커밋으로 실체화하라**: 조합 중 글자는 IME 안에 두고, 변화 하나
하나를 *실체화해 둔 것을 지우고 새 모양을 커밋* 으로 표현한다 — 새 글이 옛 글을 확장하는 경우는
꼬리만 커밋하는 것으로 줄인다. 일 이 이ㄹ 로 보이던 것도, 앱 자신의 선택 보고에 조합이 무너지던
것도, 조합 영역이 있는 척한 데서 나왔다.

### 15a.2 파이프는 둘이고 순서는 없다 — 삭제를 커밋 곁에 두라

릴레이는 텍스트 조작(`commitText`, `deleteSurroundingText*`)과 키 이벤트를 서로 다른 길로
나르고, 둘 사이의 순서는 아무도 지켜 주지 않는다. DEL 키 이벤트로 보낸 백스페이스가 나중에 낸
커밋보다 *늦게* 저쪽에 도착할 수 있다 — 그래서 다시 친 음절이 먹혔다(보고된 앉). 릴레이의
버퍼에 글자가 있음이 확인되면(`getTextBeforeCursor` 가 무언가를 돌려주면) 삭제도 커밋이 타는
**텍스트 채널**로 보내고, 키 이벤트는 버퍼가 비었거나 미상일 때의 폴백으로 남겨 두라. 빠른 두
채널보다 순서 있는 한 채널이 낫다.

### 15a.3 컨텍스트 메뉴 동작은 아무것도 안 한다

`performContextMenuAction(paste)` 는 성공한 듯 돌아오고 아무 일도 없다 — 동작할 `TextView` 가
없다. 편집 명령은 저쪽이 알아듣는 것, 곧 **키 코드**(Ctrl+A/C/X/V/Z/Y)가 되어야 하고, 단어
선택은 저쪽 자신의 단어 점프 코드(Ctrl+←, Ctrl+Shift+→)가 된다.

### 15a.4 큰 커밋은 통째로 사라질 수 있다

릴레이가 믿음직하게 전달하는 것은 타이핑이 만드는 모양 — 배치당 작은 커밋 하나다. 클립보드
전체를 담은 `commitText` 하나가 통째로 삼켜지는 것이 관측됐다. 키 하나보다 많이 넣는 것 —
패널에서 고른 클립, 날짜 타일, 로컬 텍스트 붙여넣기 — 은 **글자 단위로 타이핑해서** 넣으라:
코드포인트당 커밋 하나, 각각 제 배치로.

### 15a.5 소프트 경로는 이벤트를 하나씩 처리한다 — 코드는 물리 키보드로 입혀라

주입 코드는 성공하기 전에 두 번 실패했고, 실패마다 릴레이의 성질을 하나씩 가르쳐 줬다:

- *글자에 meta 플래그* (로컬 `TextView` 가 읽는 모양): 글자가 제어문자(Ctrl+B = 0x02)로
  해석되고 릴레이의 소프트 경로는 그것을 입력이 아니라며 거른다. 저쪽에는 Ctrl 만 도착했다.
- *진짜 수식키 프레임 + 맨 글자* (Ctrl down → B down/up → Ctrl up): 전부 도착은 하지만 소프트
  경로는 이벤트를 **하나씩** 옮긴다 — 저쪽은 Ctrl 단독 탭과 평범한 글자를 따로 받았다. 이벤트
  사이의 수식 상태는 이어지지 않는다.

물리 키보드의 Ctrl+A 는 내내 잘 됐다. 릴레이에는 하드웨어 이벤트를 위한 두 번째 경로가 있고,
그 경로는 수식 상태를 추적해 조합하기 때문이다. 답은 그 경로를 타는 것이다: **코드 시퀀스
전체를 물리 키보드의 이벤트로 입혀라** — `SOURCE_KEYBOARD`, 실제 evdev 스캔코드(A=30, C=46,
Ctrl=29 …), `FLAG_SOFT_KEYBOARD` 제거, 글자에는 meta 플래그 유지. 로컬 편집기에는 기존 소프트
모양을 그대로 쓴다 — 보통 앱에 진짜 수식키 누름을 보내면 누를 생각 없던 단축키가 깨어난다.

### 15a.6 성공처럼 보이는 신기루

코드가 죽어 있는 동안에도 Ctrl+A 는 "됐다" — 로컬 더미 `EditText` 가 스스로 전부선택을 수행하고
릴레이가 그 *효과* 를 미러링했기 때문이다. 이웃 키들이 다 죽었는데 한 키만 되는 것은 그 키의
파이프가 살아 있다는 증거가 아니다. 더미가 대신 대답한 것일 수 있다. 이 신기루가 릴리스 하나를
통째로 헛짚게 했다.

### 15a.7 저쪽 끝에서 재라

이 장의 돌파구는 전부 **원격 머신에서 키 테스터를 띄우고** 실제로 도착한 것을 본 데서 나왔다 —
"Ctrl 만", "Ctrl 따로, a 따로", "Ctrl+A 조합". 이쪽에서 추측한 결과는 그럴듯하지만 틀린 릴리스
세 번이었고, 저쪽 끝의 계측 한 번은 각 질문을 몇 분 만에 끝냈다. 원격 데스크톱 경로가 이상하면
**먼저 저쪽 끝을 계측하라.**

### 15a.8 클립보드 공유는 클라이언트의 것이고, 동작한다 — "고치기" 전에 재라

원격 데스크톱과 다른 안드로이드 앱 사이의 복사·붙여넣기가 공유되지 않는다는 신고가 있었다.
에뮬레이터의 Microsoft 클라이언트(Windows App 11.0.26071.13915)를 Windows 11 VM 에 붙이고, 세션 안의
예약 작업으로 세션 클립보드를 읽고 쓰며 쟀다(2026-09-14).

| 경우 | 결과 |
|---|---|
| 클라이언트의 클립보드 리디렉션 **끔** | 어느 방향도 넘어가지 않음 |
| 켬, 원격 복사 → 안드로이드 | 도착함. 클라이언트가 뒤에 있어도 |
| 켬, 다른 앱에서 복사 → 클라이언트로 돌아옴 | 돌아온 뒤 몇 초 안에 도착 |
| 클라이언트가 뒤에 있는 동안 다른 앱에서 복사 | 클라이언트가 앞에 올 때까지 넘어가지 않음 — 안드로이드 10+ 는 뒤에 있는 앱의 클립보드 읽기를 막는다 |
| 원격 메모장에서 키보드 액션바의 모두 선택+복사 | 원격과 안드로이드 클립보드 모두에 도착 |
| 다른 앱에서 복사, 돌아와 키보드 액션바 붙여넣기 | 원격 메모장에 붙음 |

즉 공유는 연결마다 켜는 클라이언트의 기능이고(`redirectclipboard:i:1` 이 든 URI 는 그것을 묻는다),
키보드의 조합키는 거기에 제대로 참여한다. IME 는 원격 클립보드를 전혀 읽을 수 없다. 더할 수 있는
것은 하나뿐이다. 붙여넣기에서 Ctrl+V 대신 안드로이드 클립을 쳐 넣는 것 — 리디렉션이 꺼져 있어도
넘어가지만, 원격 세션 안에서 한 복사 위에 옛 안드로이드 클립을 붙인다. 기본값이 아니라 설정으로
내놓을 일이다. 에뮬레이터에서는 클라이언트의 리디렉션 확인 창이 ATD 이미지에 없는 설정 화면으로 모든
파일 접근을 요청하다 조용히 연결을 끊는다. 먼저 `appops set --uid com.microsoft.rdc.androidx
MANAGE_EXTERNAL_STORAGE allow` 를 해 둘 것.

## 16. 릴리즈 전 점검표

- [ ] IME가 키보드 목록에 나타난다(매니페스트 권한·액션, `method.xml`이 정확한지).
- [ ] 내부 커서 장부 때문에 입력을 거부하는 코드 경로가 없다.
- [ ] **터미널** 앱(`TYPE_NULL`, 선택영역 `-1`)에서 타이핑·백스페이스·엔터가 모두 동작한다.
- [ ] `onFinishInput`, `onFinishInputView`, 서브타입 변경에서 조합을 마무리한다.
- [ ] Ctrl/Alt/Meta 조합이 앱에 도달한다. 바인딩된 IME 단축키는 그보다 먼저 가로챈다.
- [ ] 키 핸들러가 서비스 밖으로 예외를 던질 수 없다.
- [ ] 그리기 캐시 키가 레이아웃·시프트·수정자·크기·테마를 포함하고, 눌린 키는 포함하지 않는다.
- [ ] 라이트/다크 테마 모두 정상 렌더되고, 전환 시 키보드가 다시 그려진다.
- [ ] 설정 변경이 재시작 없이 살아 있는 키보드에 즉시 반영된다.
- [ ] 단위 테스트가 안드로이드 비의존 코어를 덮고, **배포되는 데이터 파일도** 파싱한다.
- [ ] 시각적·입력 상호작용 항목은 에뮬레이터가 아니라 실기기에서 확인했다.
- [ ] 타이핑·백스페이스·코드·붙여넣기를 **원격 데스크톱** 클라이언트에서 돌려 봤다(§15a).
- [ ] candidates 띠가 키가 뜬 상태에서, 붙인 모드와 떠 있는 모드 모두 보이고, 숨긴 띠는 어느
  앱에서도 자리를 차지하지 않는다(§10). 에뮬레이터 화면에는 나오지 않으므로 뷰 트리로 확인했다.
- [ ] 키보드가 가진 패널(노트)은 열려 있는 동안 모든 키와 편집 명령을 받는다(§15.31).
- [ ] 화면 키와 액션바 키가 소프트·액션바·물리 수정자와 조합되고, 누르고 있는 키는 그 누름의
  수정자로 반복된다(§15.32).
- [ ] 클립 목록이 시스템 클립보드를 양방향으로 따라가고, 키보드가 다시 켜져도 남는다(§15.33).
- [ ] `InteractionMatrixTest` 가 통과하고, 에뮬레이터에서 `scripts/interaction-matrix.sh` 가 실패 칸
  0 을 보고한다(§14).
- [ ] 12키 페이지에서 123·Move·⌫ 옆의 빗나간 탭은 한글 키나 스페이스를 치고, 16 dp 미끄러짐은
  탭이다(§15.35).
- [ ] 글자로 굴러 들어간 수정자는 그 글자에 걸리고 다음 글자에는 걸리지 않으며, 0.5 초 눌린 글자
  키는 여전히 글자다(§15.37, §15.38).
- [ ] 키보드가 가진 패널이 터미널과 채팅 앱에서 열려 **그대로 있고**, 그 위로 시스템의 띠가 여전히
  보인다(§12a, §15.52).
- [ ] 붙여넣기가 터미널에, 칸이 무엇을 보고하든 이름으로 아는 터미널에, 그리고 평범한 칸에는 편집기 자신의
  붙여넣기로 들어간다(§15.44).
- [ ] 키보드로 공유한 글은 보관되고 시스템 클립보드를 건드리지 않으며, 레이아웃은 설치하는 문으로만
  들어온다. 공유한 글이 레이아웃 머리글로 시작할 때도 그렇다(§15.46, §15.47).
- [ ] 남이 쓴 레이아웃 파일이 설치되고, 설정에 제 이름으로 나오며, 파일이 적은 키와 품은 것을 그린다.
  `docs/user-layouts.md` 의 예시가 여전히 파싱된다(§14).
- [ ] 수식키나 본 키가 실패한 원격 묶음은 누른 수식키를 모두 놓고 "보냄" 이 아니라 "불확실" 로 보고하며,
  지우기가 실패한 가나·한자 교체는 아무것도 커밋하지 않는다(§15.53, §15.54).
- [ ] 로컬 관문이 실패해야 할 때 실패한다. `tests/test-ime-instrumentation-runner.sh` 와
  `tests/test-sentence-matrix.sh` 가 통과하고, 실행기는 자기가 빌드한 flavor 를 설치한다(§15.57).
- [ ] 비밀번호 필드에서 어떤 도우미도 필드를 읽지 않고, 거기서 한 복사는 기록되지 않으며, 키보드는 첫 잠금
  해제 전에도 설정을 열지 않고 시작한다(§15.58–§15.60).
- [ ] 노트·클립·공유 글이 구분자·탭·이모지를 재시작 뒤에도 지키고, 같은 분에 만든 노트 둘은 둘로 남으며,
  열어 둔 패널이 새로 들어온 글을 지우지 않는다(§15.61~§15.63).
- [ ] 공유하거나 연 레이아웃은 Install 을 누르기 전에는 설치되지 않고, 판 번호가 다르거나 키가 너무 길거나
  이름이 없는 파일은 이유와 함께 거절된다(§15.64).
- [ ] 원격 데스크톱 클라이언트가 배치 끝이 아니라 조작마다 커서를 보고해도, 한 음절은 한 음절로 남는다
  (§15.65).
- [ ] 바뀐 내용에 맞춰 이 매뉴얼을 양쪽 언어로, 그리고 README 둘을 갱신했다.
