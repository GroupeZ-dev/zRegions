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
 * Axis-aligned box in block coordinates, with inclusive corners. Coordinates are
 * normalized at construction; containment covers {@code [min, max + 1)} on each
 * axis so the whole max block belongs to the shape.
 */
public final class CuboidShape implements RegionShape {

    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final BoundingBox boundingBox;

    public CuboidShape(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.boundingBox = new BoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.CUBOID;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x < this.maxX + 1
                && y >= this.minY && y < this.maxY + 1
                && z >= this.minZ && z < this.maxZ + 1;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    /** The 12 edges of the outer block box ({@code [min, max + 1]} on each axis). */
    @Override
    public List<Vector3> sampleBorder(double spacing) {
        double x1 = this.minX, y1 = this.minY, z1 = this.minZ;
        double x2 = this.maxX + 1, y2 = this.maxY + 1, z2 = this.maxZ + 1;
        List<Vector3> points = new ArrayList<>();
        // bottom and top rectangles
        for (double y : new double[]{y1, y2}) {
            BorderSampling.line(points, x1, y, z1, x2, y, z1, spacing);
            BorderSampling.line(points, x2, y, z1, x2, y, z2, spacing);
            BorderSampling.line(points, x2, y, z2, x1, y, z2, spacing);
            BorderSampling.line(points, x1, y, z2, x1, y, z1, spacing);
        }
        // vertical pillars
        BorderSampling.line(points, x1, y1, z1, x1, y2, z1, spacing);
        BorderSampling.line(points, x2, y1, z1, x2, y2, z1, spacing);
        BorderSampling.line(points, x2, y1, z2, x2, y2, z2, spacing);
        BorderSampling.line(points, x1, y1, z2, x1, y2, z2, spacing);
        return points;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x1", this.minX);
        map.put("y1", this.minY);
        map.put("z1", this.minZ);
        map.put("x2", this.maxX);
        map.put("y2", this.maxY);
        map.put("z2", this.maxZ);
        return map;
    }

    @Override
    public String toString() {
        return "CuboidShape{" + this.minX + "," + this.minY + "," + this.minZ
                + " -> " + this.maxX + "," + this.maxY + "," + this.maxZ + "}";
    }
}
