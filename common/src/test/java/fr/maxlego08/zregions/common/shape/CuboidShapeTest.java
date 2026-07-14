package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.ShapeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuboidShapeTest {

    @Test
    void normalizesSwappedCorners() {
        CuboidShape shape = new CuboidShape(5, 10, 5, -5, 0, -5);
        assertEquals(new BoundingBox(-5, 0, -5, 5, 10, 5), shape.getBoundingBox());
    }

    @Test
    void containsMinCornerInclusive() {
        CuboidShape shape = new CuboidShape(-5, 0, -5, 5, 10, 5);
        assertTrue(shape.contains(-5, 0, -5));
    }

    @Test
    void containsWholeMaxBlock() {
        CuboidShape shape = new CuboidShape(0, 0, 0, 5, 10, 5);
        // block (5, 10, 5) spans [5, 6) x [10, 11) x [5, 6)
        assertTrue(shape.contains(5.0, 10.0, 5.0));
        assertTrue(shape.contains(5.999, 10.999, 5.999));
        assertFalse(shape.contains(6.0, 10.0, 5.0));
        assertFalse(shape.contains(5.0, 11.0, 5.0));
        assertFalse(shape.contains(5.0, 10.0, 6.0));
    }

    @Test
    void rejectsPointsOutsideEachAxis() {
        CuboidShape shape = new CuboidShape(0, 0, 0, 10, 10, 10);
        assertFalse(shape.contains(-0.001, 5, 5));
        assertFalse(shape.contains(5, -0.001, 5));
        assertFalse(shape.contains(5, 5, -0.001));
        assertFalse(shape.contains(11.001, 5, 5));
        assertFalse(shape.contains(5, 11.001, 5));
        assertFalse(shape.contains(5, 5, 11.001));
    }

    @Test
    void containsInteriorPoint() {
        CuboidShape shape = new CuboidShape(-10, 0, -10, 10, 20, 10);
        assertTrue(shape.contains(0.5, 10.5, -3.2));
    }

    @Test
    void boundingBoxIsConstantInstance() {
        CuboidShape shape = new CuboidShape(0, 0, 0, 10, 10, 10);
        assertSame(shape.getBoundingBox(), shape.getBoundingBox());
    }

    @Test
    void serializesNormalizedCoordinates() {
        CuboidShape shape = new CuboidShape(5, 10, 5, -5, 0, -5);
        var map = shape.serialize();
        assertEquals(-5, map.get("x1"));
        assertEquals(0, map.get("y1"));
        assertEquals(-5, map.get("z1"));
        assertEquals(5, map.get("x2"));
        assertEquals(10, map.get("y2"));
        assertEquals(5, map.get("z2"));
    }

    @Test
    void hasCuboidType() {
        assertEquals(ShapeType.CUBOID, new CuboidShape(0, 0, 0, 1, 1, 1).getType());
    }
}
