package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;

import java.util.LinkedHashMap;
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
