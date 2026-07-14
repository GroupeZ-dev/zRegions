package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vertical cylinder: a circle in the XZ plane extruded between two block Y levels
 * (inclusive). Containment is a single squared-distance comparison plus a Y check
 * over {@code [minY, maxY + 1)}.
 */
public final class CylinderShape implements RegionShape {

    private final double centerX, centerZ;
    private final double radius, radiusSquared;
    private final int minY, maxY;
    private final BoundingBox boundingBox;

    public CylinderShape(double centerX, double centerZ, double radius, int minY, int maxY) {
        if (!(radius >= 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("Cylinder radius must be a positive finite number: " + radius);
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.boundingBox = new BoundingBox(
                (int) Math.floor(centerX - radius), this.minY, (int) Math.floor(centerZ - radius),
                (int) Math.ceil(centerX + radius), this.maxY, (int) Math.ceil(centerZ + radius));
    }

    @Override
    public ShapeType getType() {
        return ShapeType.CYLINDER;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        if (y < this.minY || y >= this.maxY + 1) {
            return false;
        }
        double dx = x - this.centerX;
        double dz = z - this.centerZ;
        return dx * dx + dz * dz <= this.radiusSquared;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("centerX", this.centerX);
        map.put("centerZ", this.centerZ);
        map.put("radius", this.radius);
        map.put("minY", this.minY);
        map.put("maxY", this.maxY);
        return map;
    }

    @Override
    public String toString() {
        return "CylinderShape{center=" + this.centerX + "," + this.centerZ
                + ", radius=" + this.radius + ", y=" + this.minY + ".." + this.maxY + "}";
    }
}
