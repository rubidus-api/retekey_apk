package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import com.retekey.core.HardwareSemanticMapper;
import com.retekey.core.LatinHardwareMapper;
import com.retekey.core.SemanticInput;

/** The US-keyboard tables, spot-checked against each language's Windows layout. */
public final class HardwareLayoutTablesTest {
    private static String type(KeyboardLayoutId id, String key, boolean shift) {
        HardwareSemanticMapper mapper = HardwareLayoutTables.of(id);
        assertNotNull(String.valueOf(id), mapper);
        SemanticInput input = mapper.map(key, shift);
        return input == null ? null : input.text();
    }

    @Test
    public void eachTableSitsOnItsWindowsPositions() {
        assertEquals("α", type(KeyboardLayoutId.EL_QWERTY, "hardware.key.a", false));
        assertEquals("Σ", type(KeyboardLayoutId.EL_QWERTY, "hardware.key.s", true));
        assertEquals("ς", type(KeyboardLayoutId.EL_QWERTY, "hardware.key.w", false));
        assertEquals("ש", type(KeyboardLayoutId.HE_STANDARD, "hardware.key.a", false));
        assertEquals("ק", type(KeyboardLayoutId.HE_STANDARD, "hardware.key.e", false));
        assertEquals("é", type(KeyboardLayoutId.FR_AZERTY, "hardware.keycode.9", false));
        assertEquals("ç", type(KeyboardLayoutId.FR_AZERTY, "hardware.keycode.16", false));
        assertEquals("ü", type(KeyboardLayoutId.DE_QWERTZ, "hardware.keycode.71", false));
        assertEquals("ß", type(KeyboardLayoutId.DE_QWERTZ, "hardware.keycode.69", false));
        assertEquals("ı", type(KeyboardLayoutId.TR_QWERTY, "hardware.key.i", false));
        assertEquals("ğ", type(KeyboardLayoutId.TR_QWERTY, "hardware.keycode.71", false));
        assertEquals("ñ", type(KeyboardLayoutId.ES_QWERTY, "hardware.keycode.74", false));
        assertEquals("ç", type(KeyboardLayoutId.PT_QWERTY, "hardware.keycode.74", false));
        assertEquals("è", type(KeyboardLayoutId.IT_QWERTY, "hardware.keycode.71", false));
        assertEquals("ฟ", type(KeyboardLayoutId.TH_KEDMANEE, "hardware.key.a", false));
        assertEquals("ๅ", type(KeyboardLayoutId.TH_KEDMANEE, "hardware.keycode.8", false));
        assertEquals("๑", type(KeyboardLayoutId.TH_KEDMANEE, "hardware.keycode.9", true));
        assertEquals("ो", type(KeyboardLayoutId.HI_INSCRIPT, "hardware.key.a", false));
        assertEquals("्", type(KeyboardLayoutId.HI_INSCRIPT, "hardware.key.d", false));
        assertEquals("क", type(KeyboardLayoutId.HI_INSCRIPT, "hardware.key.k", false));
    }

    @Test
    public void theNewScriptTablesSitOnTheirWindowsPositions() {
        assertEquals("й", type(KeyboardLayoutId.RU_JCUKEN, "hardware.key.q", false));
        assertEquals("ф", type(KeyboardLayoutId.RU_JCUKEN, "hardware.key.a", false));
        assertEquals("ё", type(KeyboardLayoutId.RU_JCUKEN, "hardware.keycode.68", false));
        assertEquals("і", type(KeyboardLayoutId.UK_JCUKEN, "hardware.key.s", false));
        assertEquals("ч", type(KeyboardLayoutId.BG_PHONETIC, "hardware.key.q", false));
        assertEquals("љ", type(KeyboardLayoutId.MK_STANDARD, "hardware.key.q", false));
        assertEquals("ђ", type(KeyboardLayoutId.SR_CYRILLIC, "hardware.keycode.72", false));
        assertEquals("ض", type(KeyboardLayoutId.AR_101, "hardware.key.q", false));
        assertEquals("ط", type(KeyboardLayoutId.UR_PHONETIC, "hardware.key.q", false));
        assertEquals("ღ", type(KeyboardLayoutId.KA_QWERTY, "hardware.key.q", false));
        assertEquals("ա", type(KeyboardLayoutId.HY_EASTERN, "hardware.key.a", false));
        assertEquals("Ա", type(KeyboardLayoutId.HY_EASTERN, "hardware.key.a", true));
    }

    @Test
    public void asciiAndDeadKeysAreLeftToTheEditor() {
        // Greek Q types ; / : while OEM_1 is the tonos dead key (delegated).
        assertEquals(";", type(KeyboardLayoutId.EL_QWERTY, "hardware.key.q", false));
        assertEquals(":", type(KeyboardLayoutId.EL_QWERTY, "hardware.key.q", true));
        assertNull(type(KeyboardLayoutId.EL_QWERTY, "hardware.keycode.74", false));
        assertNull(type(KeyboardLayoutId.DE_QWERTZ, "hardware.key.q", false));
        assertNull(HardwareLayoutTables.of(KeyboardLayoutId.EN_QWERTY));
        assertNull(HardwareLayoutTables.of(KeyboardLayoutId.PL_QWERTY));
    }

    @Test
    public void missingKeysAndCapsLockAreHandledPerLanguage() {
        assertEquals("i", type(KeyboardLayoutId.TR_QWERTY, "hardware.keycode.75", false));
        assertEquals("İ", type(KeyboardLayoutId.TR_QWERTY, "hardware.keycode.75", true));
        assertEquals("I", type(KeyboardLayoutId.TR_QWERTY, "hardware.key.i", true));
        assertEquals("لا", type(KeyboardLayoutId.AR_101, "hardware.key.b", false));
        assertEquals("لآ", type(KeyboardLayoutId.AR_101, "hardware.key.b", true));
        assertEquals("/", type(KeyboardLayoutId.HE_STANDARD, "hardware.key.q", false));
        assertEquals("'", type(KeyboardLayoutId.HE_STANDARD, "hardware.key.w", false));
        assertEquals("ґ", type(KeyboardLayoutId.UK_JCUKEN, "hardware.keycode.68", false));
        assertEquals("Ґ", type(KeyboardLayoutId.UK_JCUKEN, "hardware.keycode.68", true));
        assertEquals("z", type(KeyboardLayoutId.DE_QWERTZ, "hardware.key.y", false));
        assertEquals("y", type(KeyboardLayoutId.DE_QWERTZ, "hardware.key.z", false));
        assertEquals("a", type(KeyboardLayoutId.FR_AZERTY, "hardware.key.q", false));
        assertEquals("2", type(KeyboardLayoutId.FR_AZERTY, "hardware.keycode.9", true));
        assertEquals("0", type(KeyboardLayoutId.FR_AZERTY, "hardware.keycode.7", true));
        assertEquals("ู", type(KeyboardLayoutId.TH_PATTACHOTE, "hardware.keycode.13", false));
        assertEquals("ุ", type(KeyboardLayoutId.TH_PATTACHOTE, "hardware.keycode.13", true));

        // Caps Lock flips bicameral letters, respects Shift^CapsLock, and ignores digits/symbols/Thai.
        HardwareSemanticMapper tr = HardwareLayoutTables.of(KeyboardLayoutId.TR_QWERTY);
        assertEquals("İ", tr.map("hardware.keycode.75", false, true).text());
        assertEquals("i", tr.map("hardware.keycode.75", true, true).text());
        assertEquals("I", tr.map("hardware.key.i", false, true).text());
        assertEquals("Ф", HardwareLayoutTables.of(KeyboardLayoutId.RU_JCUKEN)
            .map("hardware.key.a", false, true).text());
        assertEquals("é", HardwareLayoutTables.of(KeyboardLayoutId.FR_AZERTY)
            .map("hardware.keycode.9", false, true).text());
        assertEquals("ู", HardwareLayoutTables.of(KeyboardLayoutId.TH_PATTACHOTE)
            .map("hardware.keycode.13", false, true).text());
        assertEquals("ض", HardwareLayoutTables.of(KeyboardLayoutId.AR_101)
            .map("hardware.key.q", false, true).text());
        assertEquals("A", LatinHardwareMapper.INSTANCE.map("hardware.key.a", false, true).text());
        assertEquals("a", LatinHardwareMapper.INSTANCE.map("hardware.key.a", true, true).text());
    }
}
