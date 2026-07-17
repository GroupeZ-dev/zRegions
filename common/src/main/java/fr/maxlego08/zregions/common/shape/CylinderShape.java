package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.api.shape.Vector3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** Bottom and top circles plus four vertical lines at the cardinal points. */
    @Override
    public List<Vector3> sampleBorder(double spacing) {
        double bottom = this.minY, top = this.maxY + 1;
        List<Vector3> points = new ArrayList<>();
        BorderSampling.circleXZ(points, this.centerX, bottom, this.centerZ, this.radius, spacing);
        BorderSampling.circleXZ(points, this.centerX, top, this.centerZ, this.radius, spacing);
        for (int cardinal = 0; cardinal < 4; cardinal++) {
            double angle = Math.PI / 2 * cardinal;
            double x = this.centerX + this.radius * Math.cos(angle);
            double z = this.centerZ + this.radius * Math.sin(angle);
            BorderSampling.line(points, x, bottom, z, x, top, z, spacing);
        }
        return points;
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
