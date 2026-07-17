package fr.maxlego08.zregions.common.shape;

import fr.maxlego08.zregions.api.shape.Vector3;

import java.util.List;

/**
 * Shared geometry helpers for {@code sampleBorder} implementations. Segments
 * include their start point and exclude their end, so chained edges (boxes,
 * polygons, circles) never emit duplicate corners.
 */
final class BorderSampling {

    private BorderSampling() {
    }

    /** Samples the segment [from, to) every {@code spacing} blocks. */
    static void line(List<Vector3> out, double x1, double y1, double z1,
                     double x2, double y2, double z2, double spacing) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(length / spacing));
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            out.add(new Vector3(x1 + dx * t, y1 + dy * t, z1 + dz * t));
        }
    }

    /** Samples a full horizontal circle at height {@code y}, points ~{@code spacing} apart. */
    static void circleXZ(List<Vector3> out, double centerX, double y, double centerZ,
                         double radius, double spacing) {
        int steps = circleSteps(radius, spacing);
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            out.add(new Vector3(centerX + radius * Math.cos(angle), y, centerZ + radius * Math.sin(angle)));
        }
    }

    /** Samples a full vertical circle in the XY plane (constant Z). */
    static void circleXY(List<Vector3> out, double centerX, double centerY, double z,
                         double radius, double spacing) {
        int steps = circleSteps(radius, spacing);
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            out.add(new Vector3(centerX + radius * Math.cos(angle), centerY + radius * Math.sin(angle), z));
        }
    }

    /** Samples a full vertical circle in the YZ plane (constant X). */
    static void circleYZ(List<Vector3> out, double x, double centerY, double centerZ,
                         double radius, double spacing) {
        int steps = circleSteps(radius, spacing);
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            out.add(new Vector3(x, centerY + radius * Math.sin(angle), centerZ + radius * Math.cos(angle)));
        }
    }

    private static int circleSteps(double radius, double spacing) {
        return Math.max(8, (int) Math.ceil(2 * Math.PI * radius / spacing));
    }
}
