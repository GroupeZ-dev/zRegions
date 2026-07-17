package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.api.shape.Vector3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Vertical prism over an arbitrary 2D polygon, between two block Y levels (inclusive).
 * Containment uses the even-odd ray-casting rule, so non-convex outlines (stars,
 * custom plots) are handled correctly. Vertices are defensively copied.
 */
public final class PolygonShape implements RegionShape {

    private final double[] xs;
    private final double[] zs;
    private final int minY, maxY;
    private final BoundingBox boundingBox;

    /**
     * @param points the polygon vertices as {@code [x, z]} pairs, at least 3
     * @param minY   the lowest block Y level (inclusive)
     * @param maxY   the highest block Y level (inclusive)
     */
    public PolygonShape(List<double[]> points, int minY, int maxY) {
        Objects.requireNonNull(points, "points");
        if (points.size() < 3) {
            throw new IllegalArgumentException("A polygon requires at least 3 points, got " + points.size());
        }

        int size = points.size();
        this.xs = new double[size];
        this.zs = new double[size];

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < size; i++) {
            double[] point = points.get(i);
            if (point == null || point.length < 2) {
                throw new IllegalArgumentException("Polygon point #" + i + " must be a [x, z] pair");
            }
            this.xs[i] = point[0];
            this.zs[i] = point[1];
            minX = Math.min(minX, point[0]);
            maxX = Math.max(maxX, point[0]);
            minZ = Math.min(minZ, point[1]);
            maxZ = Math.max(maxZ, point[1]);
        }

        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.boundingBox = new BoundingBox(
                (int) Math.floor(minX), this.minY, (int) Math.floor(minZ),
                (int) Math.ceil(maxX), this.maxY, (int) Math.ceil(maxZ));
    }

    @Override
    public ShapeType getType() {
        return ShapeType.POLYGON;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        if (y < this.minY || y >= this.maxY + 1) {
            return false;
        }
        boolean inside = false;
        for (int i = 0, j = this.xs.length - 1; i < this.xs.length; j = i++) {
            if ((this.zs[i] > z) != (this.zs[j] > z)
                    && x < (this.xs[j] - this.xs[i]) * (z - this.zs[i]) / (this.zs[j] - this.zs[i]) + this.xs[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    /** Bottom and top outlines of the polygon plus a vertical line at every vertex. */
    @Override
    public List<Vector3> sampleBorder(double spacing) {
        double bottom = this.minY, top = this.maxY + 1;
        List<Vector3> points = new ArrayList<>();
        for (int i = 0, j = this.xs.length - 1; i < this.xs.length; j = i++) {
            BorderSampling.line(points, this.xs[j], bottom, this.zs[j], this.xs[i], bottom, this.zs[i], spacing);
            BorderSampling.line(points, this.xs[j], top, this.zs[j], this.xs[i], top, this.zs[i], spacing);
            BorderSampling.line(points, this.xs[i], bottom, this.zs[i], this.xs[i], top, this.zs[i], spacing);
        }
        return points;
    }

    @Override
    public Map<String, Object> serialize() {
        List<List<Double>> points = new ArrayList<>(this.xs.length);
        for (int i = 0; i < this.xs.length; i++) {
            points.add(List.of(this.xs[i], this.zs[i]));
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("points", points);
        map.put("minY", this.minY);
        map.put("maxY", this.maxY);
        return map;
    }

    @Override
    public String toString() {
        return "PolygonShape{points=" + this.xs.length + ", y=" + this.minY + ".." + this.maxY + "}";
    }
}
