package fr.maxlego08.zregions.api.shape;

import java.util.Objects;

/**
 * An immutable axis-aligned bounding box, in block coordinates (inclusive).
 *
 * <p>Every {@link RegionShape} exposes one; the spatial index only ever works
 * with bounding boxes, which keeps it shape-agnostic.</p>
 */
public final class BoundingBox {

    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public BoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public int minX() {
        return this.minX;
    }

    public int minY() {
        return this.minY;
    }

    public int minZ() {
        return this.minZ;
    }

    public int maxX() {
        return this.maxX;
    }

    public int maxY() {
        return this.maxY;
    }

    public int maxZ() {
        return this.maxZ;
    }

    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x <= this.maxX + 1
                && y >= this.minY && y <= this.maxY + 1
                && z >= this.minZ && z <= this.maxZ + 1;
    }

    public int minChunkX() {
        return this.minX >> 4;
    }

    public int maxChunkX() {
        return this.maxX >> 4;
    }

    public int minChunkZ() {
        return this.minZ >> 4;
    }

    public int maxChunkZ() {
        return this.maxZ >> 4;
    }

    /** The number of chunks this box spans — drives the "large region" threshold of the index. */
    public long chunkCount() {
        long spanX = (long) (maxChunkX() - minChunkX()) + 1;
        long spanZ = (long) (maxChunkZ() - minChunkZ()) + 1;
        return spanX * spanZ;
    }

    /** Packs chunk coordinates into a single long key, the same encoding the spatial index uses. */
    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoundingBox other)) return false;
        return this.minX == other.minX && this.minY == other.minY && this.minZ == other.minZ
                && this.maxX == other.maxX && this.maxY == other.maxY && this.maxZ == other.maxZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    @Override
    public String toString() {
        return "BoundingBox{" + this.minX + "," + this.minY + "," + this.minZ
                + " -> " + this.maxX + "," + this.maxY + "," + this.maxZ + "}";
    }
}
