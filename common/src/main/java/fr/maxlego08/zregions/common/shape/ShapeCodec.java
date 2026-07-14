package fr.maxlego08.zregions.common.shape;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON codec for {@link RegionShape} parameters, backing the {@code shape_data}
 * storage column. The shape type is stored separately; this codec only encodes
 * the flat parameter map returned by {@link RegionShape#serialize()}.
 */
public final class ShapeCodec {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private ShapeCodec() {
    }

    public static String toJson(RegionShape shape) {
        Objects.requireNonNull(shape, "shape");
        return GSON.toJson(shape.serialize());
    }

    /**
     * Rebuilds a shape from its type and serialized parameters.
     *
     * @throws IllegalArgumentException if the json is malformed or misses parameters
     */
    public static RegionShape fromJson(ShapeType type, String json) {
        Objects.requireNonNull(type, "type");
        Map<String, Object> data;
        try {
            data = GSON.fromJson(json, MAP_TYPE);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("Malformed shape json: " + json, exception);
        }
        if (data == null) {
            throw new IllegalArgumentException("Malformed shape json: " + json);
        }

        return switch (type) {
            case CUBOID -> new CuboidShape(
                    getInt(data, "x1"), getInt(data, "y1"), getInt(data, "z1"),
                    getInt(data, "x2"), getInt(data, "y2"), getInt(data, "z2"));
            case CYLINDER -> new CylinderShape(
                    getDouble(data, "centerX"), getDouble(data, "centerZ"),
                    getDouble(data, "radius"), getInt(data, "minY"), getInt(data, "maxY"));
            case SPHERE -> new SphereShape(
                    getDouble(data, "centerX"), getDouble(data, "centerY"),
                    getDouble(data, "centerZ"), getDouble(data, "radius"));
            case POLYGON -> new PolygonShape(
                    getPoints(data), getInt(data, "minY"), getInt(data, "maxY"));
        };
    }

    private static double getDouble(Map<String, Object> data, String key) {
        // Gson deserializes every json number as a Double
        if (!(data.get(key) instanceof Number number)) {
            throw new IllegalArgumentException("Missing or non-numeric shape value '" + key + "'");
        }
        return number.doubleValue();
    }

    private static int getInt(Map<String, Object> data, String key) {
        return (int) getDouble(data, key);
    }

    private static List<double[]> getPoints(Map<String, Object> data) {
        if (!(data.get("points") instanceof List<?> raw)) {
            throw new IllegalArgumentException("Missing or invalid 'points' in polygon shape data");
        }
        List<double[]> points = new ArrayList<>(raw.size());
        for (Object element : raw) {
            if (!(element instanceof List<?> pair) || pair.size() < 2
                    || !(pair.get(0) instanceof Number x) || !(pair.get(1) instanceof Number z)) {
                throw new IllegalArgumentException("Polygon points must be [x, z] number pairs, got: " + element);
            }
            points.add(new double[]{x.doubleValue(), z.doubleValue()});
        }
        return points;
    }
}
