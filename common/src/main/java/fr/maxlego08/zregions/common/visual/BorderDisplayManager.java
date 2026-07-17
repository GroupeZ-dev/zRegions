package fr.maxlego08.zregions.common.visual;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.Vector3;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerAdapter;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Shows a region's border to ONE player as a particle outline (/rg show).
 * The outline follows the actual shape ({@link fr.maxlego08.zregions.api.shape.RegionShape#sampleBorder}),
 * is refreshed on a timer for a configured duration, and is spawned through
 * {@link RegionPlayer#spawnBorderParticle} — per-player packets, nobody else sees it.
 *
 * <p>One active display per player: showing another region (or the same one again)
 * replaces the previous outline. Points are sampled once at show-time; a region
 * redefined mid-display keeps its old outline until it expires — harmless.</p>
 */
public final class BorderDisplayManager {

    /** Hard ceiling on a requested duration — a typo must not schedule hours of particles. */
    public static final int MAX_DISPLAY_SECONDS = 3600;

    private final ZRegionsPlugin plugin;
    private final Map<UUID, SchedulerTask> activeDisplays = new ConcurrentHashMap<>();

    public BorderDisplayManager(ZRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts (or restarts) the border display of {@code region} for {@code player}.
     * Returns the effective display duration in seconds (for the confirmation message).
     *
     * @param requestedSeconds the caller-chosen duration, clamped to
     *                         {@value #MAX_DISPLAY_SECONDS}; zero or negative uses
     *                         the configured default ({@code borders.display-seconds})
     */
    public int show(RegionPlayer player, Region region, int requestedSeconds) {
        UUID playerId = player.getUniqueId();
        hide(playerId);

        ZRegionsConfiguration configuration = this.plugin.getConfiguration();
        List<Vector3> points = capPoints(
                region.getShape().sampleBorder(configuration.getBorderPointSpacing()),
                configuration.getBorderMaxPoints());
        int seconds = requestedSeconds > 0
                ? Math.min(requestedSeconds, MAX_DISPLAY_SECONDS)
                : configuration.getBorderDisplaySeconds();
        long refreshMillis = configuration.getBorderRefreshMillis();
        long endAt = System.currentTimeMillis() + seconds * 1000L;

        SchedulerAdapter scheduler = this.plugin.getBootstrap().getScheduler();
        SchedulerTask task = scheduler.asyncRepeating(() -> {
            if (System.currentTimeMillis() >= endAt || !player.isOnline()) {
                hide(playerId);
                return;
            }
            // particle packets go out from the game thread (region thread on Folia)
            scheduler.executeSync(() -> {
                for (Vector3 point : points) {
                    player.spawnBorderParticle(point.x(), point.y(), point.z());
                }
            });
        }, refreshMillis, TimeUnit.MILLISECONDS);
        this.activeDisplays.put(playerId, task);
        return seconds;
    }

    /** Stops the player's active display, if any. Safe to call from anywhere. */
    public void hide(UUID playerId) {
        SchedulerTask task = this.activeDisplays.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    /** A huge region must thin its outline, never flood the client: keep every Nth point. */
    private static List<Vector3> capPoints(List<Vector3> points, int maxPoints) {
        if (points.size() <= maxPoints) {
            return points;
        }
        int stride = (points.size() + maxPoints - 1) / maxPoints;
        List<Vector3> capped = new ArrayList<>(maxPoints);
        for (int i = 0; i < points.size(); i += stride) {
            capped.add(points.get(i));
        }
        return capped;
    }
}
