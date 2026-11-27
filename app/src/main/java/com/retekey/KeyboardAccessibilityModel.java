package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.retekey.core.ControlKey;
import com.retekey.core.ModifierLatches;
import com.retekey.core.SemanticInput;

/**
 * Pure-Java accessibility projection of a {@link KeyboardLayout} for {@link
 * KeyboardAccessibilityProvider}. Produces stable virtual IDs, exact cell bounds, spoken
 * descriptions, and toggle/checkable states so TalkBack key exploration can be verified on the JVM.
 */
final class KeyboardAccessibilityModel {
    static final class KeyNode {
        final int virtualId;
        final int rowIndex;
        final int keyIndex;
        final String stableKeyId;
        final String label;
        final String description;
        final int left;
        final int top;
        final int right;
        final int bottom;
        final boolean enabled;
        final boolean clickable;
        final boolean longClickable;
        final boolean checkable;
        final boolean checked;

        KeyNode(
                int virtualId,
                int rowIndex,
                int keyIndex,
                String stableKeyId,
                String label,
                String description,
                int left,
                int top,
                int right,
                int bottom,
                boolean enabled,
                boolean clickable,
                boolean longClickable,
                boolean checkable,
                boolean checked) {
            this.virtualId = virtualId;
            this.rowIndex = rowIndex;
            this.keyIndex = keyIndex;
            this.stableKeyId = stableKeyId;
            this.label = label;
            this.description = description;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.enabled = enabled;
            this.clickable = clickable;
            this.longClickable = longClickable;
            this.checkable = checkable;
            this.checked = checked;
        }
    }

    private KeyboardAccessibilityModel() {
    }

    static List<KeyNode> nodes(
            KeyboardLayout layout, int width, int height, int sdkInt,
            ModifierCoordinator modifiers) {
        if (layout == null || width <= 0 || height <= 0) {
            return Collections.emptyList();
        }
        List<List<SoftwareKeySpec>> rows = layout.rows();
        List<KeyNode> result = new ArrayList<>();
        for (int r = 0; r < rows.size(); r++) {
            List<SoftwareKeySpec> row = rows.get(r);
            int top = layout.rowEdge(r, height);
            int bottom = layout.rowEdge(r + 1, height);
            for (int k = 0; k < row.size(); k++) {
                SoftwareKeySpec key = row.get(k);
                int startCol = layout.startColumn(r, k);
                int left = layout.columnEdge(startCol, width);
                int right = layout.columnEdge(startCol + key.columnSpan(), width);
                int virtualId = r * 100 + k + 1;
                boolean actionEnabled = key.enabled() || key.isControl();
                boolean longClickable = actionEnabled
                    && (key.hasLongPress() || key.hasLongPressControl());
                ControlKey control = key.control();
                boolean checkable = isToggleControl(control);
                boolean checked = isToggleActive(control, modifiers);
                String description = descriptionFor(key, sdkInt, modifiers);
                result.add(new KeyNode(
                    virtualId, r, k, key.stableKeyId(), key.label(), description,
                    left, top, right, bottom, actionEnabled, actionEnabled,
                    longClickable, checkable, checked));
            }
        }
        return result;
    }

    static KeyNode findByVirtualId(List<KeyNode> nodes, int virtualId) {
        for (KeyNode node : nodes) {
            if (node.virtualId == virtualId) {
                return node;
            }
        }
        return null;
    }

    static KeyNode hitTest(List<KeyNode> nodes, float x, float y) {
        for (KeyNode node : nodes) {
            if (x >= node.left && x < node.right && y >= node.top && y < node.bottom) {
                return node;
            }
        }
        return null;
    }

    private static boolean isToggleControl(ControlKey control) {
        if (control == null) {
            return false;
        }
        return control == ControlKey.SHIFT || control == ControlKey.CAPS_LOCK
            || ModifierLatches.handles(control);
    }

    private static boolean isToggleActive(ControlKey control, ModifierCoordinator modifiers) {
        if (control == null || modifiers == null) {
            return false;
        }
        if (control == ControlKey.SHIFT) {
            return modifiers.shiftActive();
        }
        if (ModifierLatches.handles(control)) {
            return modifiers.modifierActive(control);
        }
        return false;
    }

    static String descriptionFor(
            SoftwareKeySpec key, int sdkInt, ModifierCoordinator modifiers) {
        if (key == null) {
            return "";
        }
        if (key.isControl()) {
            ControlKey control = key.control();
            switch (control) {
                case SHIFT:
                    if (modifiers != null && modifiers.shiftLocked()) {
                        return "Shift locked";
                    }
                    return modifiers != null && modifiers.shiftActive() ? "Shift on" : "Shift";
                case CTRL:
                    return "Control";
                case ALT:
                    return "Alt";
                case META:
                    return "Meta";
                case RSHIFT:
                    return "Right Shift";
                case CAPS_LOCK:
                    return "Caps Lock";
                case LAYOUT_TOGGLE:
                    return "Switch layout";
                case SPECIAL_CHARS_LAYER:
                    return "Symbols";
                case SPECIAL_KEYS_LAYER:
                    return "Keypad";
                case MENU_LAYER:
                    return "Menu";
                case HANJA:
                    return "Hanja";
                case IPA_FIND:
                    return "Search IPA";
                case IPA_CHART:
                    return "IPA chart";
                default:
                    break;
            }
        }
        SemanticInput input = key.semanticInput();
        if (input != null) {
            if (input.kind() == SemanticInput.Kind.DELETE_BACKWARD) {
                return "Backspace";
            }
            if (input.kind() == SemanticInput.Kind.PRIMARY_ACTION) {
                return "Enter";
            }
            if (input.kind() == SemanticInput.Kind.FLUSH) {
                return "Next";
            }
            if (" ".equals(input.text())) {
                return "Space";
            }
        }
        String label = key.label();
        switch (label) {
            case "⌫": return "Backspace";
            case "⏎": return "Enter";
            case "⇧": return "Shift";
            case "⇧•": return "Shift locked";
            case "space":
            case "⌴":
                return "Space";
            case "\uD83C\uDF10": return "Switch layout";
            case "☰": return "Menu";
            case "←": return "Left";
            case "→": return "Right";
            case "↑": return "Up";
            case "↓": return "Down";
            default:
                return LegacyGlyphs.label(label, sdkInt);
        }
    }
}
