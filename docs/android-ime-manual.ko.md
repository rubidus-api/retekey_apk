# 안드로이드 IME 구현 매뉴얼

[English](android-ime-manual.md) · **한국어**

ReteKey를 만들며 겪은 실패를 바탕으로 정리한 안드로이드 입력기(IME) 구현 실무 지침이다. IME 구현에
관한 내용만 다룬다. 규칙을 설명하는 절에는 그 규칙을 낳은 실패도 함께 적었다. 다시 유도하기 어려운
쪽은 규칙이 아니라 실패이기 때문이다.

> **이 문서는 살아 있는 문서다.** IME 구현이 바뀔 때마다 — 콜백이 추가되거나, 에디터와의 상호작용이
> 달라지거나, 실기기에서 새로운 함정을 발견할 때마다 — 최신 내용으로 갱신한다. 여기 적힌 동작이 코드와
> 다르다면 틀린 쪽은 이 문서다.
>
> (영어판이 기준 문서이고, 이 문서는 그 번역본이다.)

## 차례

- [1. 서비스 생명주기](#1-서비스-생명주기)
- [2. InputConnection 계약](#2-inputconnection-계약)
- [3. 에디터가 권위자다](#3-에디터가-권위자다)
- [4. 에디터 종류와 미상 선택영역](#4-에디터-종류와-미상-선택영역)
- [5. 조합 중인 글자](#5-조합-중인-글자)
- [6. 물리 키보드](#6-물리-키보드)
- [7. 후보 뷰](#7-후보-뷰)
- [8. 커스텀 키보드 그리기](#8-커스텀-키보드-그리기)
- [9. 테마](#9-테마)
- [10. 테스트와 검증](#10-테스트와-검증)
- [11. 안티패턴과 그것을 가르쳐 준 실패들](#11-안티패턴과-그것을-가르쳐-준-실패들)
- [12. 릴리즈 전 점검표](#12-릴리즈-전-점검표)

## 1. 서비스 생명주기

IME는 `InputMethodService`를 상속한다. 실제로 구현하게 되는 콜백:

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
  **전부**에서 할 것([§5](#5-조합-중인-글자)).

## 2. InputConnection 계약

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

## 3. 에디터가 권위자다

가장 중요한 설계 규칙이자, 가장 비싸게 배운 규칙이다.

**텍스트와 커서의 주인은 에디터다. IME가 아는 값은 언제든 틀릴 수 있는 힌트일 뿐이다.** 선택 갱신은
늦게, 순서가 뒤바뀌어, 합쳐져서 오거나 아예 오지 않는다. `-1`을 보고하는 에디터도 있고, 예측과 다르게
편집을 적용하는 에디터도 있다.

따라서:

- **수동적 커서 캐시**를 둘 것: 자기 편집 후에는 낙관적으로 갱신하고, `onUpdateSelection`이 오면
  무조건 덮어쓰게 한다. AOSP LatinIME 모델이다.
- 캐시가 에디터와 어긋난다는 이유로 입력을 거부하는 상태에 **절대** 들어가지 말 것. "동기화 깨짐" 상태
  자체가 있어선 안 된다. 화해가 안 되면 에디터가 보고한 값을 채택하고 계속 간다.
- 미상을 허용할 것: `-1` 선택영역은 오류가 아니라 정상이다.

```java
// 옳음: 에디터가 말한 것을 채택해 화해한다.
void onUpdateSelection(int oldStart, int oldEnd, int newStart, int newEnd, int csStart, int csEnd) {
    if (newStart < 0 || newEnd < 0) {
        cursor = EditorBounds.unknown();   // 미상은 정상 상태다
        return;
    }
    cursor = EditorBounds.of(newStart, newEnd, csStart, csEnd);
}
```

반대로 했을 때 무슨 일이 벌어지는지는
[§11.1](#111-빠져나올-수-없는-엄격한-기대-원장)을 볼 것.

## 4. 에디터 종류와 미상 선택영역

에디터는 `onStartInput`에서 `EditorInfo`로 한 번 분류한다.

- `inputType == InputType.TYPE_NULL` — 이 에디터는 텍스트 편집이 아니라 **원시 키 이벤트**를 원한다.
  터미널 에뮬레이터, 일부 게임/콘솔 뷰가 그렇다. `sendKeyEvent`로 키를 보낸다.
- 그 외는 리치 텍스트 에디터다. `commitText` / `setComposingText`가 적절하다.

터미널은 추가로 `initialSelStart == -1`을 보고하고 의미 있는 `onUpdateSelection`을 보내지 않는 경우가
많다. **선택영역을 알아야만 하는 키보드는 터미널에서 타이핑할 수 없다.**

규칙:

- 삽입이나 백스페이스를 "선택영역을 안다"는 조건으로 막지 말 것.
- `performContextMenuAction`이 동작한다고 가정하지 말 것. 터미널은 무시한다.
- 에디터에 액션이 있다고 가정하지 말 것. 액션이 없고 한 줄짜리라면 실제 `KEYCODE_ENTER`가 올바른
  폴백이다([§11.3](#113-원시-엔터를-거부한-것)).

## 5. 조합 중인 글자

상태를 가진 조합기(한글, 병음, 가나)는 진행 중인 음절을 `setComposingText`로 보여 주고
`finishComposingText`로, 혹은 다음 음절을 커밋하며 확정한다.

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

## 6. 물리 키보드

물리 키는 애플리케이션보다 **먼저** `onKeyDown` / `onKeyUp` / `onKeyMultiple`으로 온다. `true`를
반환한 키는 앱이 영영 받지 못한다.

**수정자 조합은 통과시킬 것.** 수정자 키 자체와 Ctrl / Alt / Meta가 걸린 모든 조합은 애플리케이션
단축키다. IME가 삼키면 Ctrl+A / Ctrl+C / Ctrl+V가 모든 앱에서 죽는다.

```java
static boolean passThroughToApp(boolean isModifierKey, boolean ctrl, boolean alt, boolean meta) {
    return isModifierKey || ctrl || alt || meta;   // Shift는 제외: Shift+글자는 그냥 텍스트다
}
```

반대로 조합키를 *직접 보낼* 때(소프트 Ctrl, 리맵된 키)는 메타 상태를 담은 실제 `KeyEvent`를 만들어
`sendKeyEvent`로 보낸다. 양쪽 모두에서 통한다. 리치 에디터는 `onKeyShortcut`으로 Ctrl+A를 전체선택으로
처리하고, 터미널은 제어 코드를 받는다.

```java
int meta = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
ic.sendKeyEvent(new KeyEvent(down, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_B, 0, meta));
ic.sendKeyEvent(new KeyEvent(down, now, KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_B, 0, meta));
```

사용자가 물리 키를 IME 기능(언어 전환, 변환 키)에 할당하게 한다면, 그 검사를 통과 검사보다 **먼저**
해야 한다. 그러지 않으면 오른쪽 Ctrl 같은 키는 보기도 전에 앱으로 위임된다. 좌우 수정자는 서로 다른
키코드(`KEYCODE_CTRL_LEFT`=113, `KEYCODE_CTRL_RIGHT`=114)라는 점이 "오른쪽 Ctrl은 한/영, 왼쪽 Ctrl은
그대로 Ctrl+C"를 가능하게 한다.

수정자 단독 바인딩은 **키를 뗄 때** 캡처할 것. 누르는 시점에는 Ctrl 단독인지 Ctrl+Space의 시작인지
구분할 수 없다.

## 7. 후보 뷰

`onCreateCandidatesView()`와 `setCandidatesViewShown(true/false)`는 키보드 뷰와 독립적인 막대를
제공한다. 이것이 중요한 이유는 **입력 뷰가 숨겨져 있어도 후보 뷰는 표시되기 때문**이다 — 물리 키보드가
붙어 있을 때가 정확히 그 상황이다. 변환 UI는 키보드 뷰가 아니라 여기에 두어야 한다.

뷰는 지연 생성되므로 순서를 지킬 것:

```java
private void showCandidates(String reading, List<Item> items) {
    pending = items;                    // onCreateCandidatesView가 쓰도록 보관
    setCandidatesViewShown(true);       // 여기서 onCreateCandidatesView가 불릴 수 있다
    if (candidatesView != null) {
        candidatesView.show(reading, items);
    }
}
```

사용자가 다른 키를 누르면, 그리고 `onStartInput` / `onFinishInputView`에서 막대를 숨길 것. 그러지
않으면 참조하던 텍스트보다 오래 살아남는다.

## 8. 커스텀 키보드 그리기

캔버스로 그리는 키보드는 키를 누를 때마다 다시 그린다. 문제가 되는 비용은
*다시 그리는 빈도 × 한 번 그리는 복잡도*이므로, 프레임당 작업량을 바뀐 것에 비례하게 유지해야 한다.

**정적인 키보드를 캐시할 것.** 눌리지 않은 키보드 — 키 모양, 입체감, 라벨 — 를 `Bitmap`에 한 번 렌더한
뒤, 매 프레임 그 비트맵을 붙이고 눌린 키의 오버레이만 그린다.

```java
protected void onDraw(Canvas canvas) {
    ensureBaseBitmap(getWidth(), getHeight());   // 시그니처가 바뀔 때만 재생성
    canvas.drawBitmap(baseBitmap, 0, 0, null);
    drawPressFeedback(canvas);                   // 둥근 사각형 하나
}
```

캐시 키에는 그 정적 이미지가 의존하는 모든 것이 들어가야 한다. 페이지, 레이아웃, 시프트 상태, 무장된
수정자, 키패드 모드, 크기, 테마. 그리고 눌린 키는 **들어가면 안 된다.** 넣으면 키 누를 때마다 비트맵
전체를 다시 만들게 된다.

그 밖의 규칙:

- `onDraw`에서 아무것도 할당하지 말 것 — `Paint`도, `Shader`도, 박싱도. 필드를 재사용한다.
- 입체감은 흐림 처리로 비싼 `setShadowLayer`보다 1–2px 어긋난 둥근 사각형으로 낼 것.
- `onDetachedFromWindow`에서 캐시 비트맵을 recycle할 것.

터치 처리에도 규칙이 있다. **탭은 시작한 키의 것이다.** `ACTION_DOWN`에서 키를 정하고, `ACTION_UP`에서
여전히 그 키 위일 때만 커밋한다. 경계 근처 탭이 옆 키를 누르지 않도록 키 사이에 데드존을 두려면, 터치를
격자 셀 전체가 아니라 키의 *보이는 면*과 비교하면 된다.

## 9. 테마

색은 하드코딩하지 말고 기기 테마에서 해석할 것.

- 라이트/다크를 존중할 것: `Configuration.uiMode & UI_MODE_NIGHT_MASK`.
- 색조를 고르지 말고 Material 역할로 배정할 것 — 배경은 surface, 키는 올라온 surface, 라벨은
  on-surface, 활성 키는 primary, 눌림은 primary 스테이트 레이어.
- Android 12+에서는 사용자의 동적 팔레트(`android.R.color.system_neutral1_*`, `system_accent1_*`)를
  쓰고, 구버전이나 해석 실패 시 다듬어 둔 라이트/다크 팔레트로 폴백할 것.
- 그리기 캐시 키에 테마를 포함시켜 라이트/다크 전환 시 키보드가 다시 그려지게 할 것.
- 그리는 모든 표면에 하나의 팔레트를 쓸 것: 키, 롱프레스 팝업, 후보 막대.

## 10. 테스트와 검증

**입력 코어를 안드로이드 비의존으로 유지할 것.** 이벤트 정규화, 레이아웃 기하, 조합 오토마타, 사전,
설정값 클램프는 모두 순수 자바로 만들 수 있고, 그러면 기기 없이 JVM에서 단위 테스트할 수 있다. 실제
로직 버그는 거의 다 여기서 잡힌다.

픽스처만이 아니라 **배포되는 데이터**도 테스트할 것. 실제 사전 파일을 단위 테스트에서 파싱해 알려진
변환 몇 개를 검증하면, 잘못된 재생성이 빌드를 깨뜨린다.

**헤드리스 에뮬레이터를 IME 동작의 판정 기준으로 믿지 말 것.** KVM 없는 헤드리스 에뮬레이터에서 반복
실측된 두 가지 실패 양상:

- IME 창이 `Requested w=0 h=0`, `mViewVisibility=GONE`, `mHasSurface=false`로 보고되고 `screencap`은
  빈 프레임버퍼를 돌려준다 — *실기기에서 정상 동작이 확인된 빌드에서도 똑같이*. 이것으로 "키보드가 안
  뜬다"고 결론지을 수 없다.
- 주입한 키 이벤트(`adb shell input keyevent`)가 IME의 `onKeyDown`에 아예 도달하지 않을 수 있고,
  실행할 때마다 결과가 달라지기까지 한다.

헤드리스 에뮬레이터가 *증명할 수 있는 것*: APK 설치, IME 등록·선택 가능, 액티비티 인플레이트,
그리고 크래시가 없다는 것(`logcat`의 `FATAL`/예외). 시각적이거나 입력 상호작용이 필요한 것은 반드시
실기기에서 확인해야 한다.

회귀가 의심될 때 유용한 기법: **동작이 확인된 태그**를 빌드해 같은 에뮬레이터에 설치해 볼 것. 그
빌드에서도 증상이 같다면, 그 증상은 내 변경이 아니라 환경이다.

## 11. 안티패턴과 그것을 가르쳐 준 실패들

### 11.1 빠져나올 수 없는 엄격한 기대 원장

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

### 11.2 삽입을 "선택영역을 안다"로 막은 것

**만든 것.** 캐시된 bounds가 미상이면 `commitText`와 `setComposingText`를 `INVALID_SELECTION`으로
거부했다.

**벌어진 일.** 터미널은 `-1`을 보고하므로 모든 키가 드롭됐고 매번 실패 토스트가 떴다 — 그 앱에서는
키보드가 완전히 고장 난 것처럼 보였다.

**해결.** 삽입은 커서 기준이라 bounds가 필요 없다. `deleteSurroundingText`도 마찬가지다. 백스페이스
역시 같은 조건으로 막혀 있었고, 그 게이트를 완전히 제거하고서야 터미널에서 동작했다.

**규칙.** *절대* 위치를 다루는 연산만 선택영역을 요구할 수 있다 — 그리고 실무적으로 그런 연산은 없어야
한다.

### 11.3 원시 엔터를 거부한 것

**만든 것.** 엔터는 에디터 액션으로 해석됐고, 액션이 없는 한 줄짜리 에디터에서는 빈 결과가 되어 아무
일도 하지 않았다. 게다가 "리치" 에디터에 대해서는 원시 엔터를 추가로 거부했다.

**벌어진 일.** 터미널에서 엔터가 전혀 동작하지 않았다 — 줄바꿈도, 전송도 없었다.

**해결.** 최종 폴백으로 `sendKeyEvent`를 통해 실제 `KEYCODE_ENTER`를 보낸다. 실제 엔터 키는 모든
에디터가 이해한다. 터미널은 줄을 전송하고, 평범한 필드는 기본 동작을 한다.

**규칙.** "의미"로 매핑되는 키에는 원시 키 폴백을 남겨 둘 것. 사용자가 누른 키에 대해 아무 일도 하지
않는 것은 결코 정답이 아니다.

### 11.4 예측 불일치마다 조합을 재앵커한 것

**만든 것.** `onUpdateSelection`에서 보고된 커서가 예측과 다르면 조합기가 `finishComposingText()`를
호출하고 리셋했다. 사용자가 다른 곳을 탭했을 때 재앵커하려는 의도였다.

**벌어진 일.** 실제 에디터는 예측과 정확히 일치하지 않는다. 불일치는 IME *자신의* 편집에서도 터졌고,
그래서 키를 칠 때마다 finish+reset이 발생해 `onUpdateSelection`을 재진입시킨 것으로 보이며, IME가
죽었다. **키보드가 아예 뜨지 않게 됐다.** 이 상태로 배포됐고 되돌려야 했다.

**해결.** 수동적 모델로 복귀. 재앵커는 명확한 신호 — 커서가 조합 영역 밖으로 나간 것 — 로만 트리거해야
하며, "예측이 정확하지 않았다"로 트리거해선 안 된다.

**규칙.** 예측 불일치로 파괴적 상태 변경을 일으키지 말 것. 예측은 힌트다. 사용자에게 보이는 상태를
초기화하는 것은 명백한 증거일 때뿐이어야 한다.

### 11.5 예외를 밖으로 흘린 것

**벌어진 일.** 키나 선택 갱신을 처리하다 던져진 예외가 IME 프로세스를 죽였고, 타이핑 도중 키보드가
사라졌다.

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

### 11.6 애플리케이션 단축키를 삼킨 것

**벌어진 일.** IME가 수정자 조합을 소비해서, 키보드가 활성인 동안 어느 앱에서도 Ctrl+A / Ctrl+C /
Ctrl+V가 동작하지 않았다.

**해결.** 수정자 키와 Ctrl/Alt/Meta 조합은 `super.onKeyDown`으로 위임할 것 — 단, 사용자가 IME 기능에
바인딩한 특정 조합은 그보다 먼저 검사해야 한다.

### 11.7 소프트 수정자를 편집 명령에만 매핑한 것

**만든 것.** 소프트 Ctrl + 글자를 컨텍스트 메뉴 동작으로 매핑했는데, `a/c/v/x/z/y`에 대해서만이었다.
나머지 글자는 그대로 텍스트로 입력됐다.

**벌어진 일.** 터미널에서 Ctrl+B가 `0x02`를 보내는 대신 글자 `b`를 입력했다.

**해결.** *모든* 글자에 대해 실제 키 조합을 보낼 것. 리치 에디터는 `onKeyShortcut`으로 Ctrl+A/C/V/X/Z/Y를
처리하고 터미널은 제어 코드를 받는다 — 특수 케이스 표 대신 하나의 메커니즘이다.

**규칙.** 효과를 흉내 내기보다 실제 입력 이벤트를 내보내는 쪽을 택할 것. 이벤트는 내가 만든 효과 표보다
더 많은 곳에서 통한다.

### 11.8 키를 칠 때마다 키보드 이미지를 통째로 다시 만든 것

**비용.** 키 하나 바뀌는 변화를 위해 30–40개 키의 그라디언트·그림자·텍스트를 매번 UI 스레드에서 다시
계산하게 된다.

**해결.** [§8](#8-커스텀-키보드-그리기) — 정적 이미지를 캐시하고 키 하나만 다시 그린다. 눌린 키는 캐시
키에 넣지 않는다.

### 11.9 색을 하드코딩한 것

**벌어진 일.** 고정 팔레트는 시스템이 다크 모드로 바뀌는 순간 어색해졌고, 사용자 테마를 전혀 반영하지
못했다.

**해결.** [§9](#9-테마) — 테마에서 해석하고, 캐시 키에 테마를 포함시킬 것.

### 11.10 에뮬레이터를 믿은 것

**벌어진 일.** 헤드리스 에뮬레이터가 크기 0에 한 번도 렌더되지 않은 IME 창을 보여 주고 주입한 키
이벤트를 흘렸는데, 이를 코드 회귀로 오인했다. 동작이 확인된 태그를 빌드해 같은 에뮬레이터에 설치하자
동일한 증상이 재현되어, 원인이 환경임이 드러났다.

**규칙.** "키보드가 고장 났다"고 결론짓기 전에, 동작이 확인된 빌드로 재현해 볼 것.
[§10](#10-테스트와-검증) 참고.

## 12. 릴리즈 전 점검표

- [ ] 내부 커서 장부 때문에 입력을 거부하는 코드 경로가 없다.
- [ ] **터미널** 앱(`TYPE_NULL`, 선택영역 `-1`)에서 타이핑·백스페이스·엔터가 모두 동작한다.
- [ ] `onFinishInput`, `onFinishInputView`, 서브타입 변경에서 조합을 마무리한다.
- [ ] Ctrl/Alt/Meta 조합이 앱에 도달한다. 바인딩된 IME 단축키는 그보다 먼저 가로챈다.
- [ ] 키 핸들러가 서비스 밖으로 예외를 던질 수 없다.
- [ ] 그리기 캐시 키가 레이아웃·시프트·수정자·크기·테마를 포함하고, 눌린 키는 포함하지 않는다.
- [ ] 라이트/다크 테마 모두 정상 렌더되고, 전환 시 키보드가 다시 그려진다.
- [ ] 단위 테스트가 안드로이드 비의존 코어를 덮고, **배포되는 데이터 파일도** 파싱한다.
- [ ] 시각적·입력 상호작용 항목은 에뮬레이터가 아니라 실기기에서 확인했다.
- [ ] 바뀐 내용에 맞춰 이 매뉴얼을 갱신했다.
