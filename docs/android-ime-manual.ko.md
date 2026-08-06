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
- [13. 설정과 영속화](#13-설정과-영속화)
- [14. 테스트와 검증](#14-테스트와-검증)
- [15. 안티패턴과 그것을 가르쳐 준 실패들](#15-안티패턴과-그것을-가르쳐-준-실패들)
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

**규칙.** "사용자가 커서를 옮겼는가"는 에디터가 단언하는 증거 — 에디터 자신의 조합 영역 —
로만 판정하라. 예측한 위치와의 비교로는 절대 판정하지 말라. 그리고 조합을 버릴 때는 먼저
있던 자리에 정착시켜라. 텍스트는 커서를 따라 이동해서는 안 된다.

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
- [ ] 바뀐 내용에 맞춰 이 매뉴얼을 갱신했다.
