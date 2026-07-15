package dev.hellgates.retekeyime;

import android.view.KeyEvent;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * The Android-side mapping from a platform-neutral {@link RawKey}/{@link KeyModifier} to concrete
 * key codes and meta-state bits. These are compile-time constants, so they read correctly even in
 * the unit-test android stub (a real {@code KeyEvent} instance is not constructed).
 */
public final class RawKeyMappingTest {
    @Test
    public void everyRawKeyMapsToItsKeyCode() {
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_LEFT,
            InputConnectionEditorBridge.keyCodeFor(RawKey.LEFT));
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT,
            InputConnectionEditorBridge.keyCodeFor(RawKey.RIGHT));
        Assert.assertEquals(KeyEvent.KEYCODE_MOVE_HOME,
            InputConnectionEditorBridge.keyCodeFor(RawKey.HOME));
        Assert.assertEquals(KeyEvent.KEYCODE_MOVE_END,
            InputConnectionEditorBridge.keyCodeFor(RawKey.END));
        Assert.assertEquals(KeyEvent.KEYCODE_PAGE_UP,
            InputConnectionEditorBridge.keyCodeFor(RawKey.PAGE_UP));
        Assert.assertEquals(KeyEvent.KEYCODE_ESCAPE,
            InputConnectionEditorBridge.keyCodeFor(RawKey.ESCAPE));
        Assert.assertEquals(KeyEvent.KEYCODE_TAB,
            InputConnectionEditorBridge.keyCodeFor(RawKey.TAB));
        Assert.assertEquals(KeyEvent.KEYCODE_F5,
            InputConnectionEditorBridge.keyCodeFor(RawKey.F5));
        Assert.assertEquals(KeyEvent.KEYCODE_MENU,
            InputConnectionEditorBridge.keyCodeFor(RawKey.MENU));
        Assert.assertEquals(KeyEvent.KEYCODE_ENTER,
            InputConnectionEditorBridge.keyCodeFor(RawKey.ENTER));
        Assert.assertEquals(KeyEvent.KEYCODE_DEL,
            InputConnectionEditorBridge.keyCodeFor(RawKey.BACKSPACE));
    }

    @Test
    public void everyRawKeyHasAMapping() {
        for (RawKey key : RawKey.values()) {
            Assert.assertNotEquals(
                key + " must map to a real key code",
                KeyEvent.KEYCODE_UNKNOWN,
                InputConnectionEditorBridge.keyCodeFor(key)
            );
        }
    }

    @Test
    public void noModifiersMeanNoMetaState() {
        Assert.assertEquals(0, InputConnectionEditorBridge.metaStateFor(Collections.emptySet()));
    }

    @Test
    public void eachModifierSetsItsMetaBit() {
        Assert.assertTrue(hasBit(EnumSet.of(KeyModifier.CTRL), KeyEvent.META_CTRL_ON));
        Assert.assertTrue(hasBit(EnumSet.of(KeyModifier.ALT), KeyEvent.META_ALT_ON));
        Assert.assertTrue(hasBit(EnumSet.of(KeyModifier.SHIFT), KeyEvent.META_SHIFT_ON));
        Assert.assertTrue(hasBit(EnumSet.of(KeyModifier.META), KeyEvent.META_META_ON));

        int both = InputConnectionEditorBridge.metaStateFor(
            EnumSet.of(KeyModifier.CTRL, KeyModifier.ALT)
        );
        Assert.assertTrue((both & KeyEvent.META_CTRL_ON) != 0);
        Assert.assertTrue((both & KeyEvent.META_ALT_ON) != 0);
    }

    private static boolean hasBit(EnumSet<KeyModifier> mods, int bit) {
        return (InputConnectionEditorBridge.metaStateFor(mods) & bit) != 0;
    }
}
