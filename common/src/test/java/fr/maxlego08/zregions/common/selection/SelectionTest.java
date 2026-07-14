package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.shape.CuboidShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionTest {

    @Test
    void emptySelectionIsIncomplete() {
        Selection selection = new Selection(null, null);
        assertFalse(selection.isComplete());
        assertFalse(selection.isSameWorld());
        assertNull(selection.getWorldName());
        assertTrue(selection.toCuboid().isEmpty());
    }

    @Test
    void singlePositionIsIncomplete() {
        Selection selection = new Selection(new RegionLocation("world", 1, 2, 3), null);
        assertFalse(selection.isComplete());
        assertEquals("world", selection.getWorldName());
        assertTrue(selection.toCuboid().isEmpty());
    }

    @Test
    void differentWorldsYieldNoCuboid() {
        Selection selection = new Selection(
                new RegionLocation("world", 0, 0, 0),
                new RegionLocation("world_nether", 10, 10, 10));
        assertTrue(selection.isComplete());
        assertFalse(selection.isSameWorld());
        assertTrue(selection.toCuboid().isEmpty());
    }

    @Test
    void completeSelectionBuildsCuboidFromBlockCoordinates() {
        Selection selection = new Selection(
                new RegionLocation("world", 10.7, 5.2, -3.9),
                new RegionLocation("world", -2.1, 20.9, 8.0));
        assertTrue(selection.isComplete());
        assertTrue(selection.isSameWorld());
        assertEquals("world", selection.getWorldName());

        CuboidShape cuboid = selection.toCuboid().orElseThrow();
        // block coords: (10, 5, -4) and (-3, 20, 8), normalized by the cuboid
        assertEquals(new BoundingBox(-3, 5, -4, 10, 20, 8), cuboid.getBoundingBox());
        assertTrue(cuboid.contains(0, 10, 0));
        assertFalse(cuboid.contains(11.5, 10, 0));
    }
}
