package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.Vector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link RegionShape#sampleBorder}: every sampled point must lie on the
 * shape's actual boundary (within a small epsilon) and inside the outer block
 * bounds, and the outline density must follow the requested spacing.
 */
class BorderSampleTest {

    private static final double SPACING = 0.5;
    private static final double EPSILON = 1.0E-6;

    @Test
    void cuboidPointsSitOnTheOuterBoxFaces() {
        CuboidShape shape = new CuboidShape(0, 10, 0, 9, 19, 4);
        List<Vector3> points = shape.sampleBorder(SPACING);

        assertFalse(points.isEmpty());
        for (Vector3 point : points) {
            boolean onX = near(point.x(), 0) || near(point.x(), 10);
            boolean onY = near(point.y(), 10) || near(point.y(), 20);
            boolean onZ = near(point.z(), 0) || near(point.z(), 5);
            // every edge point belongs to at least two faces of the outer box
            assertTrue((onX ? 1 : 0) + (onY ? 1 : 0) + (onZ ? 1 : 0) >= 2,
                    "not on an edge: " + point);
        }
        assertWithinOuterBounds(shape, points);
    }

    @Test
    void cuboidSpacingDrivesTheDensity() {
        CuboidShape shape = new CuboidShape(0, 0, 0, 9, 9, 9);
        int dense = shape.sampleBorder(0.25).size();
        int sparse = shape.sampleBorder(2.0).size();
        assertTrue(dense > sparse, "smaller spacing must produce more points");
        // 12 edges of length 10: at 2.0 spacing that is 5 points per edge
        assertEquals(12 * 5, sparse);
    }

    @Test
    void cylinderPointsSitOnTheRadiusOrTheCaps() {
        CylinderShape shape = new CylinderShape(100.5, -20.5, 12, 30, 50);
        List<Vector3> points = shape.sampleBorder(SPACING);

        assertFalse(points.isEmpty());
        for (Vector3 point : points) {
            double dx = point.x() - 100.5;
            double dz = point.z() - (-20.5);
            assertEquals(12, Math.sqrt(dx * dx + dz * dz), EPSILON, "not on the cylinder wall: " + point);
            assertTrue(point.y() >= 30 - EPSILON && point.y() <= 51 + EPSILON, "outside the Y span: " + point);
        }
    }

    @Test
    void spherePointsSitOnTheSurface() {
        SphereShape shape = new SphereShape(0, 64, 0, 20);
        List<Vector3> points = shape.sampleBorder(SPACING);

        assertFalse(points.isEmpty());
        Vector3 center = new Vector3(0, 64, 0);
        for (Vector3 point : points) {
            assertEquals(20, point.distance(center), EPSILON, "not on the sphere surface: " + point);
        }
    }

    @Test
    void polygonPointsSitOnTheEdgesOrVerticals() {
        // a right triangle: edges y=0..z axis-aligned plus one diagonal
        PolygonShape shape = new PolygonShape(
                List.of(new double[]{0, 0}, new double[]{10, 0}, new double[]{0, 10}), 5, 15);
        List<Vector3> points = shape.sampleBorder(SPACING);

        assertFalse(points.isEmpty());
        for (Vector3 point : points) {
            boolean onBottomOrTop = near(point.y(), 5) || near(point.y(), 16);
            boolean onVertexColumn = (near(point.x(), 0) && near(point.z(), 0))
                    || (near(point.x(), 10) && near(point.z(), 0))
                    || (near(point.x(), 0) && near(point.z(), 10));
            assertTrue(onBottomOrTop || onVertexColumn, "floating point: " + point);
            if (onBottomOrTop && !onVertexColumn) {
                boolean onAxisEdge = near(point.x(), 0) || near(point.z(), 0);
                boolean onDiagonal = near(point.x() + point.z(), 10);
                assertTrue(onAxisEdge || onDiagonal, "not on a polygon edge: " + point);
            }
        }
    }

    @Test
    void zeroRadiusShapesStillSampleWithoutCrashing() {
        assertFalse(new CylinderShape(0, 0, 0, 0, 10).sampleBorder(SPACING).isEmpty());
        assertFalse(new SphereShape(0, 0, 0, 0).sampleBorder(SPACING).isEmpty());
        assertFalse(new CuboidShape(3, 3, 3, 3, 3, 3).sampleBorder(SPACING).isEmpty());
    }

    private static void assertWithinOuterBounds(RegionShape shape, List<Vector3> points) {
        BoundingBox box = shape.getBoundingBox();
        for (Vector3 point : points) {
            assertTrue(point.x() >= box.minX() - EPSILON && point.x() <= box.maxX() + 1 + EPSILON
                            && point.y() >= box.minY() - EPSILON && point.y() <= box.maxY() + 1 + EPSILON
                            && point.z() >= box.minZ() - EPSILON && point.z() <= box.maxZ() + 1 + EPSILON,
                    "outside the outer bounding box: " + point);
        }
    }

    private static boolean near(double value, double expected) {
        return Math.abs(value - expected) < EPSILON;
    }
}
