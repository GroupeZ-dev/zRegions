package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.shape.CylinderShape;
import fr.maxlego08.zregions.common.shape.PolygonShape;
import fr.maxlego08.zregions.common.shape.SphereShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives every {@link ShapeType} from a player's selection — the single place
 * the create/redefine commands share (plan §7):
 *
 * <ul>
 *   <li><b>Cuboid</b>: the two positions are opposite corners.</li>
 *   <li><b>Cylinder</b>: pos1 = center (XZ), radius = horizontal distance to
 *       pos2, height = the two Y levels.</li>
 *   <li><b>Sphere</b>: pos1 = center, radius = 3D distance to pos2.</li>
 *   <li><b>Polygon</b>: the added points are the vertices (block centers),
 *       height = the two Y levels of pos1/pos2.</li>
 * </ul>
 *
 * <p>Positions snap to block centers so a wand click and a standing position
 * produce the same geometry.</p>
 */
public final class SelectionShapeBuilder {

    /** Radii below this are a pos1≈pos2 mistake, not a real selection. */
    private static final double MIN_RADIUS = 1.0;

    public enum Error {
        /** Both positions are required (and, for polygons, give the height). */
        INCOMPLETE,
        /** Positions/vertices span several worlds. */
        WORLD_MISMATCH,
        /** pos1 and pos2 are too close to derive a radius. */
        RADIUS_TOO_SMALL,
        /** A polygon needs at least 3 added points. */
        POINTS_NEEDED
    }

    /** Either a shape with its world, or the error explaining what is missing. */
    public record Result(RegionShape shape, String worldName, Error error) {

        static Result ok(RegionShape shape, String worldName) {
            return new Result(shape, worldName, null);
        }

        static Result fail(Error error) {
            return new Result(null, null, error);
        }

        public boolean isSuccess() {
            return this.error == null;
        }
    }

    private SelectionShapeBuilder() {
    }

    public static Result build(Selection selection, ShapeType type) {
        return switch (type) {
            case CUBOID -> buildCuboid(selection);
            case CYLINDER -> buildCylinder(selection);
            case SPHERE -> buildSphere(selection);
            case POLYGON -> buildPolygon(selection);
        };
    }

    private static Result buildCuboid(Selection selection) {
        if (!selection.isComplete()) {
            return Result.fail(Error.INCOMPLETE);
        }
        if (!selection.isSameWorld()) {
            return Result.fail(Error.WORLD_MISMATCH);
        }
        return Result.ok(selection.toCuboid().orElseThrow(), selection.getWorldName());
    }

    private static Result buildCylinder(Selection selection) {
        if (!selection.isComplete()) {
            return Result.fail(Error.INCOMPLETE);
        }
        if (!selection.isSameWorld()) {
            return Result.fail(Error.WORLD_MISMATCH);
        }
        RegionLocation pos1 = selection.getPos1();
        RegionLocation pos2 = selection.getPos2();
        double centerX = pos1.getBlockX() + 0.5;
        double centerZ = pos1.getBlockZ() + 0.5;
        double dx = (pos2.getBlockX() + 0.5) - centerX;
        double dz = (pos2.getBlockZ() + 0.5) - centerZ;
        double radius = Math.sqrt(dx * dx + dz * dz);
        if (radius < MIN_RADIUS) {
            return Result.fail(Error.RADIUS_TOO_SMALL);
        }
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        return Result.ok(new CylinderShape(centerX, centerZ, radius, minY, maxY), selection.getWorldName());
    }

    private static Result buildSphere(Selection selection) {
        if (!selection.isComplete()) {
            return Result.fail(Error.INCOMPLETE);
        }
        if (!selection.isSameWorld()) {
            return Result.fail(Error.WORLD_MISMATCH);
        }
        RegionLocation pos1 = selection.getPos1();
        RegionLocation pos2 = selection.getPos2();
        double centerX = pos1.getBlockX() + 0.5;
        double centerY = pos1.getBlockY() + 0.5;
        double centerZ = pos1.getBlockZ() + 0.5;
        double dx = (pos2.getBlockX() + 0.5) - centerX;
        double dy = (pos2.getBlockY() + 0.5) - centerY;
        double dz = (pos2.getBlockZ() + 0.5) - centerZ;
        double radius = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (radius < MIN_RADIUS) {
            return Result.fail(Error.RADIUS_TOO_SMALL);
        }
        return Result.ok(new SphereShape(centerX, centerY, centerZ, radius), selection.getWorldName());
    }

    private static Result buildPolygon(Selection selection) {
        List<RegionLocation> vertices = selection.getPoints();
        if (vertices.size() < 3) {
            return Result.fail(Error.POINTS_NEEDED);
        }
        // pos1/pos2 give the vertical span
        if (!selection.isComplete()) {
            return Result.fail(Error.INCOMPLETE);
        }
        if (!selection.isSameWorld()) {
            return Result.fail(Error.WORLD_MISMATCH);
        }
        String worldName = selection.getWorldName();
        List<double[]> points = new ArrayList<>(vertices.size());
        for (RegionLocation vertex : vertices) {
            if (!vertex.getWorldName().equals(worldName)) {
                return Result.fail(Error.WORLD_MISMATCH);
            }
            points.add(new double[]{vertex.getBlockX() + 0.5, vertex.getBlockZ() + 0.5});
        }
        int minY = Math.min(selection.getPos1().getBlockY(), selection.getPos2().getBlockY());
        int maxY = Math.max(selection.getPos1().getBlockY(), selection.getPos2().getBlockY());
        return Result.ok(new PolygonShape(points, minY, maxY), worldName);
    }
}
