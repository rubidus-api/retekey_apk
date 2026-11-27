package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import com.retekey.core.ControlKey;

/** Spoken labels, bounds, toggle states, and hit-testing for TalkBack virtual key nodes. */
public final class KeyboardAccessibilityModelTest {
    @Test
    public void nodesCoverLayoutWithUniqueIdsHumanDescriptionsAndHitTesting() {
        KeyboardLayout qwerty = KeyboardLayouts.of(KeyboardLayoutId.EN_QWERTY, false);
        ModifierCoordinator modifiers = new ModifierCoordinator();
        modifiers.tap(ControlKey.CTRL);
        modifiers.hold(ControlKey.SHIFT, null);

        List<KeyboardAccessibilityModel.KeyNode> nodes =
            KeyboardAccessibilityModel.nodes(qwerty, 1080, 720, 33, modifiers);

        int expectedKeys = 0;
        for (List<SoftwareKeySpec> row : qwerty.rows()) {
            expectedKeys += row.size();
        }
        assertEquals(expectedKeys, nodes.size());

        Set<Integer> ids = new HashSet<>();
        KeyboardAccessibilityModel.KeyNode shiftNode = null;
        KeyboardAccessibilityModel.KeyNode ctrlNode = null;
        KeyboardAccessibilityModel.KeyNode spaceNode = null;
        KeyboardAccessibilityModel.KeyNode aNode = null;
        for (KeyboardAccessibilityModel.KeyNode node : nodes) {
            assertTrue(node.virtualId > 0);
            assertTrue("unique virtual id", ids.add(node.virtualId));
            assertTrue(node.left >= 0 && node.right <= 1080 && node.left < node.right);
            assertTrue(node.top >= 0 && node.bottom <= 720 && node.top < node.bottom);
            assertEquals(node, KeyboardAccessibilityModel.findByVirtualId(nodes, node.virtualId));
            assertEquals(node, KeyboardAccessibilityModel.hitTest(
                nodes, (node.left + node.right) / 2f, (node.top + node.bottom) / 2f));
            if ("touch.modifier.shift".equals(node.stableKeyId)) {
                shiftNode = node;
            } else if ("touch.modifier.ctrl".equals(node.stableKeyId)) {
                ctrlNode = node;
            } else if ("touch.text.space".equals(node.stableKeyId)) {
                spaceNode = node;
            } else if ("touch.en.a".equals(node.stableKeyId)) {
                aNode = node;
            }
        }
        assertNotNull(shiftNode);
        assertEquals("Shift locked", shiftNode.description);
        assertTrue(shiftNode.checkable);
        assertTrue(shiftNode.checked);

        assertNotNull(ctrlNode);
        assertEquals("Control", ctrlNode.description);
        assertTrue(ctrlNode.checkable);
        assertTrue(ctrlNode.checked);

        assertNotNull(spaceNode);
        assertEquals("Space", spaceNode.description);
        assertFalse(spaceNode.checkable);

        assertNotNull(aNode);
        assertEquals("a", aNode.description);
        assertTrue(aNode.longClickable);
    }
}
