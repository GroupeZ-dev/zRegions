package fr.maxlego08.zregions.api.shape;

/**
 * The supported region shapes.
 */
public enum ShapeType {

    /** Axis-aligned box: squares, rectangles and cubes. */
    CUBOID,

    /** Vertical cylinder: circles and discs extruded on the Y axis. */
    CYLINDER,

    /** Full sphere. */
    SPHERE,

    /** Vertical prism over an arbitrary 2D polygon (stars, custom outlines). */
    POLYGON
}
