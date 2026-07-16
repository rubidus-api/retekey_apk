package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.hellgates.retekeyime.HardwareKeyBindings.Binding;
import java.util.List;
import org.junit.Test;

public final class HardwareKeyBindingsTest {
    // A few Android key codes used as fixtures (no android.jar dependency in unit tests).
    private static final int SPACE = 62;
    private static final int CTRL_RIGHT = 114;
    private static final int F9 = 139;

    @Test
    public void parseRoundTripsFormat() {
        List<Binding> bindings = HardwareKeyBindings.parse("1:62,0:114");
        assertEquals(2, bindings.size());
        assertEquals(new Binding(HardwareKeyBindings.MOD_SHIFT, SPACE), bindings.get(0));
        assertEquals(new Binding(0, CTRL_RIGHT), bindings.get(1));
        assertEquals("1:62,0:114", HardwareKeyBindings.format(bindings));
    }

    @Test
    public void parseSkipsMalformedTokensAndDeduplicates() {
        List<Binding> bindings = HardwareKeyBindings.parse(" , x:y , 0:114 , 0:114 , 3: , :5 ");
        assertEquals(1, bindings.size());
        assertEquals(new Binding(0, CTRL_RIGHT), bindings.get(0));
    }

    @Test
    public void chordRequiresItsModifier() {
        List<Binding> bindings = HardwareKeyBindings.parse("1:62"); // Shift+Space
        assertTrue(HardwareKeyBindings.matches(bindings, SPACE, HardwareKeyBindings.MOD_SHIFT));
        // Extra held modifiers are tolerated.
        assertTrue(HardwareKeyBindings.matches(bindings, SPACE,
            HardwareKeyBindings.MOD_SHIFT | HardwareKeyBindings.MOD_CTRL));
        // Plain Space (no shift) must not toggle.
        assertFalse(HardwareKeyBindings.matches(bindings, SPACE, 0));
    }

    @Test
    public void loneKeyMatchesRegardlessOfModifiers() {
        List<Binding> bindings = HardwareKeyBindings.parse("0:114"); // Right Ctrl
        assertTrue(HardwareKeyBindings.matches(bindings, CTRL_RIGHT, 0));
        assertTrue(HardwareKeyBindings.matches(bindings, CTRL_RIGHT, HardwareKeyBindings.MOD_CTRL));
        assertFalse(HardwareKeyBindings.matches(bindings, F9, 0));
    }

    @Test
    public void addIgnoresDuplicates() {
        List<Binding> bindings = HardwareKeyBindings.parse("0:139");
        HardwareKeyBindings.add(bindings, new Binding(0, F9));
        assertEquals(1, bindings.size());
    }

    @Test
    public void modsOfPacksBits() {
        assertEquals(0, HardwareKeyBindings.modsOf(false, false, false, false));
        assertEquals(HardwareKeyBindings.MOD_SHIFT | HardwareKeyBindings.MOD_ALT,
            HardwareKeyBindings.modsOf(true, false, true, false));
    }
}
