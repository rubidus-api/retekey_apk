package com.retekey;

import java.util.Collections;
import java.util.EnumSet;
import org.junit.Assert;
import org.junit.Test;
import com.retekey.core.KeyModifier;
import com.retekey.core.RawKey;
import com.retekey.core.RemoteDesktopKeys;
import com.retekey.core.SemanticInput;

/**
 * In remote-desktop mode a printable ASCII character goes out as the US key that types it, the
 * way a US keyboard with other caps on it would send it (owner, 2026-11-18).
 */
public class RemoteDesktopKeysTest {

    private static void assertKey(String text, RawKey key, boolean shift) {
        SemanticInput input = RemoteDesktopKeys.forText(text);
        Assert.assertNotNull(text, input);
        Assert.assertEquals(text, SemanticInput.Kind.RAW_KEY, input.kind());
        Assert.assertEquals(text, key, input.rawKey());
        Assert.assertTrue(text + " must go out as a physical key", input.isPhysical());
        Assert.assertEquals(text,
            shift ? EnumSet.of(KeyModifier.SHIFT) : Collections.<KeyModifier>emptySet(),
            input.modifiers());
    }

    @Test
    public void unshiftedSymbolsAreTheirOwnKeys() {
        assertKey("[", RawKey.LEFT_BRACKET, false);
        assertKey("]", RawKey.RIGHT_BRACKET, false);
        assertKey(";", RawKey.SEMICOLON, false);
        assertKey("'", RawKey.APOSTROPHE, false);
        assertKey(",", RawKey.COMMA, false);
        assertKey(".", RawKey.PERIOD, false);
        assertKey("/", RawKey.SLASH, false);
        assertKey("\\", RawKey.BACKSLASH, false);
        assertKey("`", RawKey.GRAVE, false);
        assertKey("-", RawKey.MINUS, false);
        assertKey("=", RawKey.EQUALS, false);
        assertKey(" ", RawKey.SPACE, false);
    }

    @Test
    public void shiftedSymbolsAreTheUsKeyWithShift() {
        assertKey("!", RawKey.DIGIT_1, true);
        assertKey("@", RawKey.DIGIT_2, true);
        assertKey(")", RawKey.DIGIT_0, true);
        assertKey("{", RawKey.LEFT_BRACKET, true);
        assertKey("}", RawKey.RIGHT_BRACKET, true);
        assertKey(":", RawKey.SEMICOLON, true);
        assertKey("\"", RawKey.APOSTROPHE, true);
        assertKey("<", RawKey.COMMA, true);
        assertKey(">", RawKey.PERIOD, true);
        assertKey("?", RawKey.SLASH, true);
        assertKey("|", RawKey.BACKSLASH, true);
        assertKey("~", RawKey.GRAVE, true);
        assertKey("_", RawKey.MINUS, true);
        assertKey("+", RawKey.EQUALS, true);
    }

    @Test
    public void digitsAndLettersAreKeysToo() {
        assertKey("0", RawKey.DIGIT_0, false);
        assertKey("7", RawKey.DIGIT_7, false);
        assertKey("q", RawKey.Q, false);
        assertKey("Q", RawKey.Q, true);
    }

    @Test
    public void everyPrintableAsciiCharacterHasAKeyWithAScanCode() {
        for (char c = 0x20; c < 0x7f; c++) {
            SemanticInput input = RemoteDesktopKeys.forText(String.valueOf(c));
            Assert.assertNotNull("U+" + Integer.toHexString(c), input);
            Assert.assertNotEquals("scan code for " + c, 0,
                InputConnectionEditorBridge.scanCodeFor(input.rawKey()));
        }
    }

    @Test
    public void anythingElseStaysText() {
        Assert.assertNull(RemoteDesktopKeys.forText("÷"));
        Assert.assertNull(RemoteDesktopKeys.forText("ㅂ"));
        Assert.assertNull(RemoteDesktopKeys.forText("ab"));
        Assert.assertNull(RemoteDesktopKeys.forText(""));
        Assert.assertNull(RemoteDesktopKeys.forText(null));
    }

    @Test
    public void newKeysHaveTheirEvdevScanCodes() {
        Assert.assertEquals(41, InputConnectionEditorBridge.scanCodeFor(RawKey.GRAVE));
        Assert.assertEquals(12, InputConnectionEditorBridge.scanCodeFor(RawKey.MINUS));
        Assert.assertEquals(13, InputConnectionEditorBridge.scanCodeFor(RawKey.EQUALS));
        Assert.assertEquals(26, InputConnectionEditorBridge.scanCodeFor(RawKey.LEFT_BRACKET));
        Assert.assertEquals(27, InputConnectionEditorBridge.scanCodeFor(RawKey.RIGHT_BRACKET));
        Assert.assertEquals(43, InputConnectionEditorBridge.scanCodeFor(RawKey.BACKSLASH));
        Assert.assertEquals(39, InputConnectionEditorBridge.scanCodeFor(RawKey.SEMICOLON));
        Assert.assertEquals(40, InputConnectionEditorBridge.scanCodeFor(RawKey.APOSTROPHE));
        Assert.assertEquals(51, InputConnectionEditorBridge.scanCodeFor(RawKey.COMMA));
        Assert.assertEquals(53, InputConnectionEditorBridge.scanCodeFor(RawKey.SLASH));
        Assert.assertEquals(2, InputConnectionEditorBridge.scanCodeFor(RawKey.DIGIT_1));
        Assert.assertEquals(10, InputConnectionEditorBridge.scanCodeFor(RawKey.DIGIT_9));
        Assert.assertEquals(11, InputConnectionEditorBridge.scanCodeFor(RawKey.DIGIT_0));
    }

    @Test
    public void theSymbolPagesEditingKeysBecomeKeyboardKeys() {
        SemanticInput backspace = RemoteDesktopKeys.asUsKey(SemanticInput.deleteBackward());
        Assert.assertEquals(RawKey.BACKSPACE, backspace.rawKey());
        Assert.assertTrue(backspace.isPhysical());
        SemanticInput enter = RemoteDesktopKeys.asUsKey(SemanticInput.primaryAction());
        Assert.assertEquals(RawKey.ENTER, enter.rawKey());
        Assert.assertTrue(enter.isPhysical());
        SemanticInput esc = RemoteDesktopKeys.asUsKey(SemanticInput.rawKey(RawKey.ESCAPE));
        Assert.assertEquals(RawKey.ESCAPE, esc.rawKey());
        Assert.assertTrue(esc.isPhysical());
    }

    @Test
    public void aSymbolOnTheSymbolPageGoesOutAsItsKey() {
        SemanticInput semicolon = RemoteDesktopKeys.asUsKey(SemanticInput.text(";"));
        Assert.assertEquals(RawKey.SEMICOLON, semicolon.rawKey());
        Assert.assertTrue(semicolon.isPhysical());
        SemanticInput divide = SemanticInput.text("÷");
        Assert.assertSame(divide, RemoteDesktopKeys.asUsKey(divide));
    }

    @Test
    public void everyTextKeyOnTheSymbolPageThatIsAsciiBecomesAKey() {
        for (java.util.List<SoftwareKeySpec> row : KeyboardLayouts.specialChars().rows()) {
            for (SoftwareKeySpec key : row) {
                if (!key.enabled()) {
                    continue;
                }
                SemanticInput out = RemoteDesktopKeys.asUsKey(key.semanticInput());
                Assert.assertEquals(key.stableKeyId(), SemanticInput.Kind.RAW_KEY, out.kind());
                Assert.assertTrue(key.stableKeyId(), out.isPhysical());
                for (String hold : key.longPressTexts()) {
                    if (hold.charAt(0) < 0x7f) {
                        Assert.assertEquals(hold, SemanticInput.Kind.RAW_KEY,
                            RemoteDesktopKeys.asUsKey(SemanticInput.text(hold)).kind());
                    }
                }
            }
        }
    }
}
