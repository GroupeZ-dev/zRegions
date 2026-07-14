package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.ShapeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SphereShapeTest {

    private final SphereShape shape = new SphereShape(0.5, 64, 0.5, 5);

    @Test
    void containsCenter() {
        assertTrue(this.shape.contains(0.5, 64, 0.5));
    }

    @Test
    void containsPointExactlyOnRadius() {
        assertTrue(this.shape.contains(0.5, 69, 0.5));
        assertTrue(this.shape.contains(5.5, 64, 0.5));
    }

    @Test
    void rejectsPointJustBeyondRadius() {
        assertFalse(this.shape.contains(0.5, 69.001, 0.5));
        assertFalse(this.shape.contains(5.501, 64, 0.5));
    }

    @Test
    void boundingBoxCornerIsOutsideSphere() {
        // the box corner is at distance radius * sqrt(3): inside the box, outside the sphere
        double offset = 5 * 0.8;
        assertTrue(this.shape.getBoundingBox().contains(0.5 + offset, 64 + offset, 0.5 + offset));
        assertFalse(this.shape.contains(0.5 + offset, 64 + offset, 0.5 + offset));
    }

    @Test
    void rejectsPointAboveAndBelow() {
        assertFalse(this.shape.contains(0.5, 69.5, 0.5));
        assertFalse(this.shape.contains(0.5, 58.5, 0.5));
    }

    @Test
    void boundingBoxCoversFullSphere() {
        // floor(0.5 - 5) = -5, ceil(0.5 + 5) = 6, floor(64 - 5) = 59, ceil(64 + 5) = 69
        assertEquals(new BoundingBox(-5, 59, -5, 6, 69, 6), this.shape.getBoundingBox());
    }

    @Test
    void rejectsNegativeRadius() {
        assertThrows(IllegalArgumentException.class, () -> new SphereShape(0, 0, 0, -1));
    }

    @Test
    void serializesParameters() {
        var map = this.shape.serialize();
        assertEquals(0.5, map.get("centerX"));
        assertEquals(64.0, map.get("centerY"));
        assertEquals(0.5, map.get("centerZ"));
        assertEquals(5.0, map.get("radius"));
    }

    @Test
    void hasSphereType() {
        assertEquals(ShapeType.SPHERE, this.shape.getType());
    }
}
