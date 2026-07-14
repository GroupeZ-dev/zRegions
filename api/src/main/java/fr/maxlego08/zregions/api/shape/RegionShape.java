package fr.maxlego08.zregions.api.shape;

import java.util.Map;

/**
 * The geometry of a region. Implementations must be immutable and cheap to query:
 * {@link #contains(double, double, double)} sits on the protection hot path and is
 * called after the chunk index has already narrowed the candidates down.
 */
public interface RegionShape {

    ShapeType getType();

    /** Precise containment test, in world coordinates. */
    boolean contains(double x, double y, double z);

    /** The axis-aligned bounds of this shape. Must be constant for a given instance. */
    BoundingBox getBoundingBox();

    /**
     * Serializes the shape parameters (not the type) to a flat map, suitable for JSON
     * storage in the {@code shape_data} column. Deserialization is handled by the
     * shape codec in the common module.
     */
    Map<String, Object> serialize();
}
