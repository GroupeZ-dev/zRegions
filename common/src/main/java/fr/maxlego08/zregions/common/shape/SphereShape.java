package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Full sphere. Containment is a single squared-distance comparison against the
 * squared radius.
 */
public final class SphereShape implements RegionShape {

    private final double centerX, centerY, centerZ;
    private final double radius, radiusSquared;
    private final BoundingBox boundingBox;

    public SphereShape(double centerX, double centerY, double centerZ, double radius) {
        if (!(radius >= 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("Sphere radius must be a positive finite number: " + radius);
        }
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;
        this.boundingBox = new BoundingBox(
                (int) Math.floor(centerX - radius), (int) Math.floor(centerY - radius), (int) Math.floor(centerZ - radius),
                (int) Math.ceil(centerX + radius), (int) Math.ceil(centerY + radius), (int) Math.ceil(centerZ + radius));
    }

    @Override
    public ShapeType getType() {
        return ShapeType.SPHERE;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        double dx = x - this.centerX;
        double dy = y - this.centerY;
        double dz = z - this.centerZ;
        return dx * dx + dy * dy + dz * dz <= this.radiusSquared;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("centerX", this.centerX);
        map.put("centerY", this.centerY);
        map.put("centerZ", this.centerZ);
        map.put("radius", this.radius);
        return map;
    }

    @Override
    public String toString() {
        return "SphereShape{center=" + this.centerX + "," + this.centerY + "," + this.centerZ
                + ", radius=" + this.radius + "}";
    }
}
