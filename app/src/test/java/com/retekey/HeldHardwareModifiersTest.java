package com.retekey;

import android.view.KeyEvent;
import java.util.EnumSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * The modifiers held on a physical keyboard, which the action bar's arrows carry. They belong to
 * the keyboard they are held on: one keyboard's key-up must not let go of another's, and a
 * keyboard unplugged, or a field left with a key still down, leaves nothing behind (review R17).
 */
public final class HeldHardwareModifiersTest {
    private static final int ONE = 7;
    private static final int OTHER = 9;

    @Test
    public void aHeldKeyIsHeldUntilItsOwnKeyUp() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        Assert.assertTrue(held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, true));
        Assert.assertEquals(EnumSet.of(KeyModifier.CTRL), held.held());
        held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, false);
        Assert.assertTrue(held.held().isEmpty());
        Assert.assertFalse(held.onKey(ONE, KeyEvent.KEYCODE_A, true));
    }

    /** Both Shifts down, one released: Shift is still held, as it is under the fingers. */
    @Test
    public void eachSideOfAPairCountsOnItsOwn() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        held.onKey(ONE, KeyEvent.KEYCODE_SHIFT_LEFT, true);
        held.onKey(ONE, KeyEvent.KEYCODE_SHIFT_RIGHT, true);
        held.onKey(ONE, KeyEvent.KEYCODE_SHIFT_LEFT, false);
        Assert.assertEquals(EnumSet.of(KeyModifier.SHIFT), held.held());
    }

    @Test
    public void oneKeyboardsUpDoesNotReleaseAnothersKey() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, true);
        held.onKey(OTHER, KeyEvent.KEYCODE_CTRL_LEFT, false);
        Assert.assertEquals(EnumSet.of(KeyModifier.CTRL), held.held());
        held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, false);
        Assert.assertTrue(held.held().isEmpty());
    }

    @Test
    public void unpluggingAKeyboardLetsGoOfWhatItHeld() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, true);
        held.onKey(OTHER, KeyEvent.KEYCODE_SHIFT_LEFT, true);
        held.forgetDevice(ONE);
        Assert.assertEquals(EnumSet.of(KeyModifier.SHIFT), held.held());
        held.clear();
        Assert.assertTrue(held.held().isEmpty());
    }

    /**
     * A key-up that never arrives — the keyboard was unplugged mid-chord, or the window changed —
     * would leave a modifier held for ever. The next ordinary key from that keyboard says what is
     * really down, and that is believed.
     */
    @Test
    public void anOrdinaryKeySaysWhatIsReallyHeld() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, true);
        held.onKey(ONE, KeyEvent.KEYCODE_ALT_LEFT, true);

        held.reconcile(ONE, KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);

        Assert.assertEquals(EnumSet.of(KeyModifier.CTRL), held.held());
    }

    /** A modifier's own event is not asked: its meta state is the platform's business. */
    @Test
    public void aModifiersOwnEventDoesNotReconcile() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, true);
        held.reconcile(ONE, KeyEvent.KEYCODE_SHIFT_LEFT, 0);
        Assert.assertEquals(EnumSet.of(KeyModifier.CTRL), held.held());
    }

    /** Another keyboard's ordinary key says nothing about this one's keys. */
    @Test
    public void reconcilingOneKeyboardLeavesAnotherAlone() {
        HeldHardwareModifiers held = new HeldHardwareModifiers();
        held.onKey(ONE, KeyEvent.KEYCODE_CTRL_LEFT, true);
        held.reconcile(OTHER, KeyEvent.KEYCODE_A, 0);
        Assert.assertEquals(EnumSet.of(KeyModifier.CTRL), held.held());
    }
}
