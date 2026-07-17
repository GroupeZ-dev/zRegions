package fr.maxlego08.zregions.api.shape;

/**
 * An immutable 3D point in world coordinates, without any platform type —
 * used by the shape API (border sampling, future geometry helpers).
 */
public record Vector3(double x, double y, double z) {

    public double distanceSquared(Vector3 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(Vector3 other) {
        return Math.sqrt(distanceSquared(other));
    }
}
