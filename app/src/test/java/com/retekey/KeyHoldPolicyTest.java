package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.retekey.core.SemanticInput;

/** Hold vs auto-repeat classification for {@link KeyHoldPolicy}. */
public final class KeyHoldPolicyTest {

    @Test
    public void flickOnlyKeyHoldsAndDoesNotAutoRepeat() {
        SoftwareKeySpec customFlickOnly = SoftwareKeySpec.enabled(
                "touch.test.flick", "x", SemanticInput.text("x"))
            .withFlicks("L", null, null, null);
        assertTrue(KeyHoldPolicy.showsFlickGuide(customFlickOnly));
        assertFalse(KeyHoldPolicy.repeatsOnHold(customFlickOnly));
        assertEquals(KeyHoldPolicy.Action.HOLD, KeyHoldPolicy.actionFor(customFlickOnly, true));

        SoftwareKeySpec kana = KeyboardLayouts.of(KeyboardLayoutId.JA_FLICK, false)
            .findById("touch.kana.ka");
        assertTrue(KeyHoldPolicy.showsFlickGuide(kana));
        assertFalse(KeyHoldPolicy.repeatsOnHold(kana));
        assertEquals(KeyHoldPolicy.Action.HOLD, KeyHoldPolicy.actionFor(kana, true));
    }

    @Test
    public void plainTypingKeyRepeatsOnlyWhenEnabled() {
        SoftwareKeySpec plain = SoftwareKeySpec.enabled(
            "touch.test.plain", "a", SemanticInput.text("a"));
        assertTrue(KeyHoldPolicy.repeatsOnHold(plain));
        assertEquals(KeyHoldPolicy.Action.REPEAT, KeyHoldPolicy.actionFor(plain, true));
        assertEquals(KeyHoldPolicy.Action.NONE, KeyHoldPolicy.actionFor(plain, false));
    }

    @Test
    public void disabledNonControlKeyDoesNothing() {
        SoftwareKeySpec disabled = SoftwareKeySpec.disabled("touch.test.disabled", " ");
        assertFalse(KeyHoldPolicy.repeatsOnHold(disabled));
        assertEquals(KeyHoldPolicy.Action.NONE, KeyHoldPolicy.actionFor(disabled, true));
    }
}
