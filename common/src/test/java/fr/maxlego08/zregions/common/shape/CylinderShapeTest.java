package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.ShapeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CylinderShapeTest {

    private final CylinderShape shape = new CylinderShape(0.5, 0.5, 10, 0, 20);

    @Test
    void containsCenter() {
        assertTrue(this.shape.contains(0.5, 10, 0.5));
    }

    @Test
    void containsPointExactlyOnRadius() {
        assertTrue(this.shape.contains(10.5, 10, 0.5));
    }

    @Test
    void rejectsPointJustBeyondRadius() {
        assertFalse(this.shape.contains(10.501, 10, 0.5));
    }

    @Test
    void boundingBoxCornerIsOutsideCircle() {
        // the corner of the bounding box is at distance radius * sqrt(2) from the center:
        // inside the box, outside the circle — the case that separates circle from square
        double cornerX = 0.5 + 10 * 0.9;
        double cornerZ = 0.5 + 10 * 0.9;
        assertTrue(this.shape.getBoundingBox().contains(cornerX, 10, cornerZ));
        assertFalse(this.shape.contains(cornerX, 10, cornerZ));
    }

    @Test
    void enforcesVerticalRange() {
        assertTrue(this.shape.contains(0.5, 0, 0.5));
        assertTrue(this.shape.contains(0.5, 20.999, 0.5)); // block maxY spans [20, 21)
        assertFalse(this.shape.contains(0.5, 21.0, 0.5));
        assertFalse(this.shape.contains(0.5, -0.001, 0.5));
    }

    @Test
    void boundingBoxCoversFullCircle() {
        // floor(0.5 - 10) = -10, ceil(0.5 + 10) = 11
        assertEquals(new BoundingBox(-10, 0, -10, 11, 20, 11), this.shape.getBoundingBox());
    }

    @Test
    void normalizesSwappedYLevels() {
        CylinderShape swapped = new CylinderShape(0, 0, 5, 30, 10);
        assertTrue(swapped.contains(0, 15, 0));
        assertEquals(10, swapped.getBoundingBox().minY());
        assertEquals(30, swapped.getBoundingBox().maxY());
    }

    @Test
    void rejectsNegativeRadius() {
        assertThrows(IllegalArgumentException.class, () -> new CylinderShape(0, 0, -1, 0, 10));
    }

    @Test
    void serializesParameters() {
        var map = this.shape.serialize();
        assertEquals(0.5, map.get("centerX"));
        assertEquals(0.5, map.get("centerZ"));
        assertEquals(10.0, map.get("radius"));
        assertEquals(0, map.get("minY"));
        assertEquals(20, map.get("maxY"));
    }

    @Test
    void hasCylinderType() {
        assertEquals(ShapeType.CYLINDER, this.shape.getType());
    }
}
