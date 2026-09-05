package dev.aether.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardDrawListTest {
    @Test
    void editorBoundsIncludeVanillaHeaderPaddingAndAllFifteenRows() {
        var list = new ScoreboardDrawList();
        list.fill(235, 130, 399, 139, 0x66000000);
        list.fill(235, 139, 399, 275, 0x4C000000);
        assertFalse(list.isEmpty());
        assertEquals(235, list.left());
        assertEquals(130, list.top());
        assertEquals(164, list.width());
        assertEquals(145, list.height());
    }

    @Test
    void transparentAndReversedFillsStillContributeToEditorBounds() {
        var list = new ScoreboardDrawList();
        list.fill(399, 139, 235, 130, 0);
        list.fill(399, 140, 235, 139, 0);
        assertFalse(list.isEmpty());
        assertEquals(164, list.width());
        assertEquals(10, list.height());
    }

    @Test
    void absentSidebarHasNoStaleBounds() {
        var list = new ScoreboardDrawList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.left());
        assertEquals(0, list.top());
        assertEquals(0, list.width());
        assertEquals(0, list.height());
    }
}
