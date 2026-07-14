package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.common.platform.RegionLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionManagerTest {

    private final SelectionManager manager = new SelectionManager();
    private final UUID playerId = UUID.randomUUID();

    @Test
    void unknownPlayerHasNoSelection() {
        assertTrue(this.manager.getSelection(this.playerId).isEmpty());
    }

    @Test
    void settingBothPositionsCompletesSelection() {
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        this.manager.setPos2(this.playerId, new RegionLocation("world", 10, 10, 10));

        Selection selection = this.manager.getSelection(this.playerId).orElseThrow();
        assertTrue(selection.isComplete());
        assertTrue(selection.toCuboid().isPresent());
    }

    @Test
    void updatingOnePositionKeepsTheOther() {
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        this.manager.setPos2(this.playerId, new RegionLocation("world", 10, 10, 10));
        this.manager.setPos1(this.playerId, new RegionLocation("world", 5, 5, 5));

        Selection selection = this.manager.getSelection(this.playerId).orElseThrow();
        assertEquals(5, selection.getPos1().getBlockX());
        assertEquals(10, selection.getPos2().getBlockX());
    }

    @Test
    void everySetSwapsInAFreshSelection() {
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        Selection first = this.manager.getSelection(this.playerId).orElseThrow();

        this.manager.setPos2(this.playerId, new RegionLocation("world", 10, 10, 10));
        Selection second = this.manager.getSelection(this.playerId).orElseThrow();

        assertNotSame(first, second);
    }

    @Test
    void selectionsAreIsolatedPerPlayer() {
        UUID other = UUID.randomUUID();
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));

        assertTrue(this.manager.getSelection(other).isEmpty());
    }

    @Test
    void clearForgetsTheSelection() {
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        this.manager.clear(this.playerId);

        assertTrue(this.manager.getSelection(this.playerId).isEmpty());
    }
}
