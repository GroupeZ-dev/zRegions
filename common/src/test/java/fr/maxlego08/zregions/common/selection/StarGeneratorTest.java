package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.common.platform.RegionLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StarGeneratorTest {

    @Test
    void generatesTwoVerticesPerBranch() {
        List<RegionLocation> vertices = StarGenerator.vertices("world", 100, 64, 200, 5, 10, 4);
        assertEquals(10, vertices.size());
    }

    @Test
    void verticesAlternateBetweenOuterAndInnerRadius() {
        List<RegionLocation> vertices = StarGenerator.vertices("world", 100, 64, 200, 5, 10, 4);
        for (int i = 0; i < vertices.size(); i++) {
            RegionLocation vertex = vertices.get(i);
            double dx = vertex.getX() - 100;
            double dz = vertex.getZ() - 200;
            double expected = i % 2 == 0 ? 10 : 4;
            assertEquals(expected, Math.sqrt(dx * dx + dz * dz), 1e-9,
                    "vertex #" + i + " should sit on the " + (i % 2 == 0 ? "outer" : "inner") + " radius");
        }
    }

    @Test
    void verticesCarryTheWorldAndHeight() {
        for (RegionLocation vertex : StarGenerator.vertices("world_the_end", 0, 80, 0, 4, 6, 2)) {
            assertEquals("world_the_end", vertex.getWorldName());
            assertEquals(80.0, vertex.getY(), 1e-9);
        }
    }

    @Test
    void rejectsTooFewBranches() {
        assertThrows(IllegalArgumentException.class, () ->
                StarGenerator.vertices("world", 0, 64, 0, StarGenerator.MIN_BRANCHES - 1, 10, 4));
    }

    @Test
    void rejectsTooManyBranches() {
        assertThrows(IllegalArgumentException.class, () ->
                StarGenerator.vertices("world", 0, 64, 0, StarGenerator.MAX_BRANCHES + 1, 10, 4));
    }

    @Test
    void rejectsInnerRadiusNotBelowOuter() {
        assertThrows(IllegalArgumentException.class, () ->
                StarGenerator.vertices("world", 0, 64, 0, 5, 10, 10));
    }

    @Test
    void rejectsNonPositiveInnerRadius() {
        assertThrows(IllegalArgumentException.class, () ->
                StarGenerator.vertices("world", 0, 64, 0, 5, 10, 0));
    }
}
