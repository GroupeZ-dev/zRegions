package fr.maxlego08.zregions.common.spatial;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.shape.CuboidShape;
import fr.maxlego08.zregions.common.shape.CylinderShape;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkRegionIndexTest {

    @Test
    void findsRegionAcrossAllOverlappedChunks() {
        ChunkRegionIndex index = new ChunkRegionIndex();
        Region region = new FakeRegion("spawn", new CuboidShape(0, 0, 0, 40, 20, 40));
        index.add(region);

        // chunks (0,0) through (2,2)
        assertEquals(List.of(region), index.query(5, 10, 5));
        assertEquals(List.of(region), index.query(20, 10, 20));
        assertEquals(List.of(region), index.query(35, 10, 35));
    }

    @Test
    void filtersRegionInChunkButOutsideShape() {
        ChunkRegionIndex index = new ChunkRegionIndex();
        // circle around (8, 8) of radius 5: chunk (0,0) is indexed, but the chunk
        // corner is well outside the circle
        index.add(new FakeRegion("arena", new CylinderShape(8, 8, 5, 0, 100)));

        assertEquals(List.of(), index.query(1, 50, 1));
        assertEquals(1, index.query(8, 50, 8).size());
    }

    @Test
    void queryOutsideAnyIndexedChunkIsEmpty() {
        ChunkRegionIndex index = new ChunkRegionIndex();
        index.add(new FakeRegion("spawn", new CuboidShape(0, 0, 0, 15, 20, 15)));

        assertEquals(List.of(), index.query(500, 10, 500));
    }

    @Test
    void returnsAllOverlappingRegions() {
        ChunkRegionIndex index = new ChunkRegionIndex();
        Region outer = new FakeRegion("outer", new CuboidShape(0, 0, 0, 30, 100, 30));
        Region inner = new FakeRegion("inner", new CuboidShape(10, 0, 10, 20, 100, 20));
        index.add(outer);
        index.add(inner);

        List<Region> hits = index.query(15, 50, 15);
        assertEquals(2, hits.size());
        assertTrue(hits.contains(outer));
        assertTrue(hits.contains(inner));

        assertEquals(List.of(outer), index.query(5, 50, 5));
    }

    @Test
    void removeStopsMatching() {
        ChunkRegionIndex index = new ChunkRegionIndex();
        Region kept = new FakeRegion("kept", new CuboidShape(0, 0, 0, 15, 20, 15));
        Region removed = new FakeRegion("removed", new CuboidShape(0, 0, 0, 15, 20, 15));
        index.add(kept);
        index.add(removed);

        index.remove(removed);

        assertEquals(List.of(kept), index.query(5, 10, 5));
    }

    @Test
    void largeRegionBypassesBucketsButIsStillFound() throws Exception {
        ChunkRegionIndex index = new ChunkRegionIndex();
        // 0..599 blocks = 38x38 chunks = 1444 chunks > LARGE_THRESHOLD_CHUNKS
        Region large = new FakeRegion("wilderness", new CuboidShape(0, 0, 0, 599, 255, 599));
        index.add(large);

        assertTrue(largeRegionsOf(index).contains(large), "region should be routed to largeRegions");
        assertEquals(List.of(large), index.query(300, 100, 300));
        assertEquals(List.of(), index.query(700, 100, 700));

        index.remove(large);
        assertEquals(List.of(), index.query(300, 100, 300));
        assertTrue(largeRegionsOf(index).isEmpty());
    }

    @Test
    void largeAndBucketedRegionsAreCombined() {
        ChunkRegionIndex index = new ChunkRegionIndex();
        Region large = new FakeRegion("wilderness", new CuboidShape(0, 0, 0, 599, 255, 599));
        Region small = new FakeRegion("shop", new CuboidShape(10, 0, 10, 20, 255, 20));
        index.add(large);
        index.add(small);

        List<Region> hits = index.query(15, 100, 15);
        assertEquals(2, hits.size());
        assertTrue(hits.contains(large));
        assertTrue(hits.contains(small));
    }

    @Test
    void clearEmptiesEverything() throws Exception {
        ChunkRegionIndex index = new ChunkRegionIndex();
        index.add(new FakeRegion("small", new CuboidShape(0, 0, 0, 15, 20, 15)));
        index.add(new FakeRegion("large", new CuboidShape(0, 0, 0, 599, 255, 599)));

        index.clear();

        assertEquals(List.of(), index.query(5, 10, 5));
        assertEquals(List.of(), index.query(300, 100, 300));
        assertTrue(largeRegionsOf(index).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static List<Region> largeRegionsOf(ChunkRegionIndex index) throws Exception {
        Field field = ChunkRegionIndex.class.getDeclaredField("largeRegions");
        field.setAccessible(true);
        return (List<Region>) field.get(index);
    }
}
