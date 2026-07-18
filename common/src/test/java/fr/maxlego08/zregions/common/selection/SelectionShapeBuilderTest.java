package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionShapeBuilderTest {

    private static RegionLocation world(double x, double y, double z) {
        return new RegionLocation("world", x, y, z);
    }

    // --- Cuboid ---

    @Test
    void cuboidFromCompleteSameWorldSelection() {
        Selection selection = new Selection(world(0, 0, 0), world(10, 10, 10));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.CUBOID);
        assertTrue(result.isSuccess());
        assertEquals(ShapeType.CUBOID, result.shape().getType());
        assertEquals("world", result.worldName());
    }

    @Test
    void cuboidRequiresBothPositions() {
        Selection selection = new Selection(world(0, 0, 0), null);

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.CUBOID);
        assertFalse(result.isSuccess());
        assertEquals(SelectionShapeBuilder.Error.INCOMPLETE, result.error());
    }

    @Test
    void cuboidRejectsCrossWorldSelection() {
        Selection selection = new Selection(world(0, 0, 0),
                new RegionLocation("world_nether", 10, 10, 10));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.CUBOID);
        assertEquals(SelectionShapeBuilder.Error.WORLD_MISMATCH, result.error());
    }

    // --- Cylinder ---

    @Test
    void cylinderDerivesRadiusFromHorizontalDistance() {
        Selection selection = new Selection(world(10, 60, 10), world(20, 80, 10));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.CYLINDER);
        assertTrue(result.isSuccess());
        assertEquals(ShapeType.CYLINDER, result.shape().getType());
        assertEquals("world", result.worldName());

        // center = block center of pos1, radius = XZ distance between block centers
        Map<String, Object> map = result.shape().serialize();
        assertEquals(10.5, (double) map.get("centerX"), 1e-9);
        assertEquals(10.5, (double) map.get("centerZ"), 1e-9);
        assertEquals(10.0, (double) map.get("radius"), 1e-9);
        assertEquals(60, map.get("minY"));
        assertEquals(80, map.get("maxY"));
    }

    @Test
    void cylinderRejectsIdenticalPositions() {
        Selection selection = new Selection(world(10, 60, 10), world(10, 80, 10));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.CYLINDER);
        assertFalse(result.isSuccess());
        assertEquals(SelectionShapeBuilder.Error.RADIUS_TOO_SMALL, result.error());
    }

    @Test
    void cylinderContainsItsCenterAndRejectsFarPoints() {
        Selection selection = new Selection(world(10, 60, 10), world(20, 80, 10));

        RegionShape shape = SelectionShapeBuilder.build(selection, ShapeType.CYLINDER).shape();
        assertTrue(shape.contains(10.5, 70, 10.5));
        assertFalse(shape.contains(10.5, 70, 40));
    }

    // --- Sphere ---

    @Test
    void sphereDerivesRadiusFromDistance() {
        Selection selection = new Selection(world(0, 0, 0), world(2, 3, 6));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.SPHERE);
        assertTrue(result.isSuccess());
        assertEquals(ShapeType.SPHERE, result.shape().getType());

        // radius = 3D distance between block centers: sqrt(2² + 3² + 6²) = 7
        Map<String, Object> map = result.shape().serialize();
        assertEquals(0.5, (double) map.get("centerX"), 1e-9);
        assertEquals(0.5, (double) map.get("centerY"), 1e-9);
        assertEquals(0.5, (double) map.get("centerZ"), 1e-9);
        assertEquals(7.0, (double) map.get("radius"), 1e-9);
    }

    @Test
    void sphereRejectsIdenticalPositions() {
        Selection selection = new Selection(world(5, 5, 5), world(5, 5, 5));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.SPHERE);
        assertFalse(result.isSuccess());
        assertEquals(SelectionShapeBuilder.Error.RADIUS_TOO_SMALL, result.error());
    }

    // --- Polygon ---

    @Test
    void polygonRequiresAtLeastThreePoints() {
        Selection selection = new Selection(world(0, 0, 0), world(0, 20, 0),
                List.of(world(0, 0, 0), world(10, 0, 0)));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.POLYGON);
        assertFalse(result.isSuccess());
        assertEquals(SelectionShapeBuilder.Error.POINTS_NEEDED, result.error());
    }

    @Test
    void polygonRequiresBothPositionsForTheHeight() {
        Selection selection = new Selection(null, null,
                List.of(world(0, 0, 0), world(10, 0, 0), world(0, 0, 10)));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.POLYGON);
        assertFalse(result.isSuccess());
        assertEquals(SelectionShapeBuilder.Error.INCOMPLETE, result.error());
    }

    @Test
    void polygonRejectsVertexInAnotherWorld() {
        Selection selection = new Selection(world(0, 0, 0), world(0, 20, 0),
                List.of(world(0, 0, 0), world(10, 0, 0),
                        new RegionLocation("world_nether", 0, 0, 10)));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.POLYGON);
        assertFalse(result.isSuccess());
        assertEquals(SelectionShapeBuilder.Error.WORLD_MISMATCH, result.error());
    }

    @Test
    void polygonContainsPointsInsideTheTriangle() {
        Selection selection = new Selection(world(0, 0, 0), world(0, 20, 0),
                List.of(world(0, 0, 0), world(10, 0, 0), world(0, 0, 10)));

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(selection, ShapeType.POLYGON);
        assertTrue(result.isSuccess());
        assertEquals(ShapeType.POLYGON, result.shape().getType());
        assertEquals("world", result.worldName());

        // triangle (0.5,0.5) (10.5,0.5) (0.5,10.5), y span [0, 21)
        assertTrue(result.shape().contains(2, 5, 2));
        assertFalse(result.shape().contains(10, 5, 10));
    }
}
