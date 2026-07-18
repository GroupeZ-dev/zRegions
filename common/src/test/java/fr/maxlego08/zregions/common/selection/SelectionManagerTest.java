package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.common.platform.RegionLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    void addPointReturnsGrowingCountAndKeepsPositions() {
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        this.manager.setPos2(this.playerId, new RegionLocation("world", 10, 10, 10));

        assertEquals(1, this.manager.addPoint(this.playerId, new RegionLocation("world", 1, 0, 1)));
        assertEquals(2, this.manager.addPoint(this.playerId, new RegionLocation("world", 2, 0, 2)));

        Selection selection = this.manager.getSelection(this.playerId).orElseThrow();
        assertEquals(2, selection.getPoints().size());
        assertEquals(0, selection.getPos1().getBlockX());
        assertEquals(10, selection.getPos2().getBlockX());
    }

    @Test
    void settingPositionsKeepsThePointList() {
        this.manager.addPoint(this.playerId, new RegionLocation("world", 1, 0, 1));
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        this.manager.setPos2(this.playerId, new RegionLocation("world", 10, 10, 10));

        Selection selection = this.manager.getSelection(this.playerId).orElseThrow();
        assertEquals(1, selection.getPoints().size());
        assertEquals(1, selection.getPoints().get(0).getBlockX());
    }

    @Test
    void clearPointsEmptiesPointsButKeepsPositions() {
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        this.manager.setPos2(this.playerId, new RegionLocation("world", 10, 10, 10));
        this.manager.addPoint(this.playerId, new RegionLocation("world", 1, 0, 1));
        this.manager.clearPoints(this.playerId);

        Selection selection = this.manager.getSelection(this.playerId).orElseThrow();
        assertTrue(selection.getPoints().isEmpty());
        assertEquals(0, selection.getPos1().getBlockX());
        assertEquals(10, selection.getPos2().getBlockX());
    }

    @Test
    void setPointsReplacesTheWholeList() {
        this.manager.addPoint(this.playerId, new RegionLocation("world", 1, 0, 1));
        this.manager.setPoints(this.playerId, List.of(
                new RegionLocation("world", 5, 0, 5),
                new RegionLocation("world", 6, 0, 6)));

        Selection selection = this.manager.getSelection(this.playerId).orElseThrow();
        assertEquals(2, selection.getPoints().size());
        assertEquals(5, selection.getPoints().get(0).getBlockX());
        assertEquals(6, selection.getPoints().get(1).getBlockX());
    }

    @Test
    void clearRemovesPositionsAndPoints() {
        this.manager.setPos1(this.playerId, new RegionLocation("world", 0, 0, 0));
        this.manager.setPos2(this.playerId, new RegionLocation("world", 10, 10, 10));
        this.manager.addPoint(this.playerId, new RegionLocation("world", 1, 0, 1));
        this.manager.clear(this.playerId);

        assertTrue(this.manager.getSelection(this.playerId).isEmpty());
    }
}
