package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolygonShapeTest {

    private static PolygonShape square() {
        return new PolygonShape(List.of(
                new double[]{0, 0},
                new double[]{10, 0},
                new double[]{10, 10},
                new double[]{0, 10}
        ), 0, 10);
    }

    /**
     * A five-pointed star centered on the origin: outer vertices at {@code outerRadius},
     * inner vertices at {@code innerRadius}, alternating every 36 degrees. Non-convex.
     */
    private static PolygonShape star(double outerRadius, double innerRadius, int minY, int maxY) {
        List<double[]> points = new ArrayList<>();
        for (int k = 0; k < 10; k++) {
            double angle = -Math.PI / 2 + k * Math.PI / 5;
            double radius = (k % 2 == 0) ? outerRadius : innerRadius;
            points.add(new double[]{Math.cos(angle) * radius, Math.sin(angle) * radius});
        }
        return new PolygonShape(points, minY, maxY);
    }

    @Test
    void squareContainsInteriorAndRejectsExterior() {
        PolygonShape shape = square();
        assertTrue(shape.contains(5, 5, 5));
        assertTrue(shape.contains(0.001, 5, 0.001));
        assertFalse(shape.contains(11, 5, 5));
        assertFalse(shape.contains(5, 5, -0.001));
        assertFalse(shape.contains(-0.001, 5, 5));
    }

    @Test
    void squareEnforcesVerticalRange() {
        PolygonShape shape = square();
        assertTrue(shape.contains(5, 0, 5));
        assertTrue(shape.contains(5, 10.999, 5)); // block maxY spans [10, 11)
        assertFalse(shape.contains(5, 11.0, 5));
        assertFalse(shape.contains(5, -0.001, 5));
    }

    @Test
    void starContainsCenterAndArms() {
        PolygonShape shape = star(10, 4, 0, 100);
        assertTrue(shape.contains(0, 50, 0));

        // along a tip axis the star extends to the outer radius
        double tipAngle = -Math.PI / 2;
        assertTrue(shape.contains(Math.cos(tipAngle) * 8, 50, Math.sin(tipAngle) * 8));
    }

    @Test
    void starNotchIsOutsideButInsideBoundingBox() {
        PolygonShape shape = star(10, 4, 0, 100);

        // along a notch axis the star stops at the inner radius: a point at radius 7
        // sits in the hollow between two arms — outside the shape, inside the box
        double notchAngle = -Math.PI / 2 + Math.PI / 5;
        double x = Math.cos(notchAngle) * 7;
        double z = Math.sin(notchAngle) * 7;
        assertTrue(shape.getBoundingBox().contains(x, 50, z));
        assertFalse(shape.contains(x, 50, z));

        // just inside the inner radius on the same axis is still inside the star
        assertTrue(shape.contains(Math.cos(notchAngle) * 3.9, 50, Math.sin(notchAngle) * 3.9));
    }

    @Test
    void boundingBoxCoversVertices() {
        PolygonShape shape = square();
        assertEquals(new BoundingBox(0, 0, 0, 10, 10, 10), shape.getBoundingBox());
    }

    @Test
    void boundingBoxFloorsAndCeilsFractionalVertices() {
        PolygonShape shape = new PolygonShape(List.of(
                new double[]{-1.5, -2.5},
                new double[]{4.5, -2.5},
                new double[]{4.5, 3.5}
        ), 5, 15);
        assertEquals(new BoundingBox(-2, 5, -3, 5, 15, 4), shape.getBoundingBox());
    }

    @Test
    void defensivelyCopiesPoints() {
        List<double[]> points = new ArrayList<>(List.of(
                new double[]{0, 0},
                new double[]{10, 0},
                new double[]{10, 10},
                new double[]{0, 10}
        ));
        PolygonShape shape = new PolygonShape(points, 0, 10);

        points.get(1)[0] = 1000;
        points.clear();

        assertTrue(shape.contains(5, 5, 5));
        assertFalse(shape.contains(11, 5, 5));
        assertEquals(new BoundingBox(0, 0, 0, 10, 10, 10), shape.getBoundingBox());
    }

    @Test
    void rejectsTooFewPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new PolygonShape(List.of(new double[]{0, 0}, new double[]{1, 1}), 0, 10));
    }

    @Test
    void rejectsMalformedPoint() {
        assertThrows(IllegalArgumentException.class, () -> new PolygonShape(List.of(
                new double[]{0, 0}, new double[]{1}, new double[]{2, 2}), 0, 10));
    }

    @Test
    void serializesParameters() {
        var map = square().serialize();
        assertEquals(0, map.get("minY"));
        assertEquals(10, map.get("maxY"));
        assertEquals(List.of(
                List.of(0.0, 0.0),
                List.of(10.0, 0.0),
                List.of(10.0, 10.0),
                List.of(0.0, 10.0)
        ), map.get("points"));
    }

    @Test
    void hasPolygonType() {
        assertEquals(ShapeType.POLYGON, square().getType());
    }
}
