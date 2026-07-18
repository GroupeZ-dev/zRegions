package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.common.platform.RegionLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the vertices of a star polygon (plan §7: a star is simply a
 * non-convex polygon — {@code 2 × branches} vertices alternating between the
 * outer and inner radius). The result feeds the selection's point list; the
 * region is then created with {@code /rg create <name> polygon}.
 */
public final class StarGenerator {

    public static final int MIN_BRANCHES = 3;
    public static final int MAX_BRANCHES = 50;

    private StarGenerator() {
    }

    /**
     * The star's vertices around {@code (centerX, centerZ)}, first branch tip
     * pointing north (negative Z). {@code y}/{@code worldName} are carried into
     * every vertex so the selection stays world-consistent.
     *
     * @throws IllegalArgumentException on branches outside [{@value #MIN_BRANCHES},
     *                                  {@value #MAX_BRANCHES}] or radii not satisfying
     *                                  {@code 0 < inner < outer}
     */
    public static List<RegionLocation> vertices(String worldName, double centerX, double y, double centerZ,
                                                int branches, double outerRadius, double innerRadius) {
        if (branches < MIN_BRANCHES || branches > MAX_BRANCHES) {
            throw new IllegalArgumentException("branches must be in [" + MIN_BRANCHES + ", " + MAX_BRANCHES + "]");
        }
        if (!(innerRadius > 0) || !(outerRadius > innerRadius) || !Double.isFinite(outerRadius)) {
            throw new IllegalArgumentException("radii must satisfy 0 < inner < outer");
        }

        List<RegionLocation> vertices = new ArrayList<>(branches * 2);
        for (int i = 0; i < branches * 2; i++) {
            double radius = i % 2 == 0 ? outerRadius : innerRadius;
            double angle = Math.PI * i / branches - Math.PI / 2;
            vertices.add(new RegionLocation(worldName,
                    centerX + radius * Math.cos(angle), y, centerZ + radius * Math.sin(angle)));
        }
        return vertices;
    }
}
