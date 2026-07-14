package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShapeCodecTest {

    /** Round-trips the shape and asserts identical behavior on a coordinate grid. */
    private static RegionShape roundTrip(RegionShape original) {
        String json = ShapeCodec.toJson(original);
        RegionShape decoded = ShapeCodec.fromJson(original.getType(), json);

        assertEquals(original.getType(), decoded.getType());
        assertEquals(original.getBoundingBox(), decoded.getBoundingBox());
        for (double x = -16; x <= 16; x += 1.5) {
            for (double y = -16; y <= 80; y += 3.5) {
                for (double z = -16; z <= 16; z += 1.5) {
                    assertEquals(original.contains(x, y, z), decoded.contains(x, y, z),
                            "contains mismatch at " + x + "," + y + "," + z);
                }
            }
        }
        return decoded;
    }

    @Test
    void roundTripsCuboid() {
        assertInstanceOf(CuboidShape.class, roundTrip(new CuboidShape(-8, 0, -8, 12, 64, 12)));
    }

    @Test
    void roundTripsCylinder() {
        assertInstanceOf(CylinderShape.class, roundTrip(new CylinderShape(0.5, -2.5, 9.75, 5, 60)));
    }

    @Test
    void roundTripsSphere() {
        assertInstanceOf(SphereShape.class, roundTrip(new SphereShape(1.5, 32, -3.5, 12.25)));
    }

    @Test
    void roundTripsPolygon() {
        PolygonShape star = new PolygonShape(List.of(
                new double[]{0, -10}, new double[]{2.35, -3.24},
                new double[]{9.51, -3.09}, new double[]{3.8, 1.24},
                new double[]{5.88, 8.09}, new double[]{0, 4},
                new double[]{-5.88, 8.09}, new double[]{-3.8, 1.24},
                new double[]{-9.51, -3.09}, new double[]{-2.35, -3.24}
        ), 0, 64);
        assertInstanceOf(PolygonShape.class, roundTrip(star));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> ShapeCodec.fromJson(ShapeType.CUBOID, "{not json"));
    }

    @Test
    void rejectsNullJson() {
        assertThrows(IllegalArgumentException.class, () -> ShapeCodec.fromJson(ShapeType.CUBOID, "null"));
    }

    @Test
    void rejectsMissingValue() {
        assertThrows(IllegalArgumentException.class,
                () -> ShapeCodec.fromJson(ShapeType.CUBOID, "{\"x1\":0,\"y1\":0,\"z1\":0}"));
    }

    @Test
    void rejectsNonNumericValue() {
        assertThrows(IllegalArgumentException.class,
                () -> ShapeCodec.fromJson(ShapeType.SPHERE, "{\"centerX\":\"a\",\"centerY\":0,\"centerZ\":0,\"radius\":1}"));
    }

    @Test
    void rejectsInvalidPolygonPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> ShapeCodec.fromJson(ShapeType.POLYGON, "{\"points\":[[0,0],[1],[2,2]],\"minY\":0,\"maxY\":10}"));
        assertThrows(IllegalArgumentException.class,
                () -> ShapeCodec.fromJson(ShapeType.POLYGON, "{\"points\":\"nope\",\"minY\":0,\"maxY\":10}"));
        assertThrows(IllegalArgumentException.class,
                () -> ShapeCodec.fromJson(ShapeType.POLYGON, "{\"points\":[[0,0],[1,1]],\"minY\":0,\"maxY\":10}"));
    }
}
