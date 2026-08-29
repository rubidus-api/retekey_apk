package com.retekey;

import java.util.EnumSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * 원격데스크톱에서 편집 명령이 되는 코드 표.
 *
 * <p>★ 이 표가 사용자 결정을 지킨다(2026-08-29): **붙여넣기도 저쪽 Ctrl+V** 다. 한동안은
 * 이 폰의 클립보드를 한 글자씩 타이핑했는데, 그것은 복사가 저쪽에 안 닿던 시절의 처방이었다 —
 * 복사가 고쳐진 지금 그 처방은 **엉뚱한 것을 붙여넣거나(폰 클립보드) 아무것도 안 한다**.
 */
public final class RemoteEditChordsTest {
    @Test
    public void everyEditCommandBecomesItsFamiliarChord() {
        Assert.assertEquals(RawKey.A, RemoteEditChords.letterFor(android.R.id.selectAll));
        Assert.assertEquals(RawKey.C, RemoteEditChords.letterFor(android.R.id.copy));
        Assert.assertEquals(RawKey.X, RemoteEditChords.letterFor(android.R.id.cut));
        Assert.assertEquals(RawKey.Z, RemoteEditChords.letterFor(EditMenuIds.UNDO));
        Assert.assertEquals(RawKey.Y, RemoteEditChords.letterFor(EditMenuIds.REDO));
    }

    @Test
    public void pasteIsTheFarSidesOwnCtrlV() {
        Assert.assertEquals(RawKey.V, RemoteEditChords.letterFor(android.R.id.paste));
    }

    @Test
    public void aCommandWithoutAChordSaysSo() {
        Assert.assertNull(RemoteEditChords.letterFor(android.R.id.startSelectingText));
    }

    @Test
    public void everyChordHoldsCtrlAndNothingElse() {
        Assert.assertEquals(EnumSet.of(KeyModifier.CTRL), RemoteEditChords.modifiers());
    }
}
