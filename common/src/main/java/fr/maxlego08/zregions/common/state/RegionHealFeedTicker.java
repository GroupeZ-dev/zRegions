package fr.maxlego08.zregions.common.state;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerAdapter;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerTask;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Applies the heal/feed region flags on a one-second tick: for every online player
 * inside a region that sets {@code heal-amount} or {@code feed-amount}, adds the
 * amount to their health / food every {@code heal-delay}/{@code feed-delay} seconds,
 * bounded by the min/max flags. Amounts may be negative (poison / starve zones). The
 * heavy work runs off-thread; the player mutations are dispatched to the game thread.
 */
public final class RegionHealFeedTicker {

    private static final long TICK_SECONDS = 1;
    private static final double MIN_DELAY_SECONDS = 0.05;

    private final ZRegionsPlugin plugin;
    private final Map<UUID, Long> lastHeal = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFeed = new ConcurrentHashMap<>();
    private SchedulerTask task;

    public RegionHealFeedTicker(ZRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        SchedulerAdapter scheduler = this.plugin.getBootstrap().getScheduler();
        this.task = scheduler.asyncRepeating(() -> scheduler.executeSync(this::tickAll), TICK_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void tickAll() {
        long now = System.currentTimeMillis();
        for (UUID id : this.plugin.getBootstrap().getOnlinePlayers()) {
            this.plugin.getBootstrap().getPlayer(id).ifPresent(player -> tickPlayer(player, now));
        }
    }

    private void tickPlayer(RegionPlayer player, long now) {
        RegionManager manager = this.plugin.getRegionManager();
        RegionLocation location = player.getLocation();
        String world = location.getWorldName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        UUID id = player.getUniqueId();

        Optional<Double> healAmount = manager.resolveFlagIfSet(world, x, y, z, Flags.HEAL_AMOUNT, id);
        if (healAmount.isPresent()
                && elapsed(this.lastHeal, id, delay(manager, world, x, y, z, Flags.HEAL_DELAY, id), now)) {
            applyHeal(player, healAmount.get(),
                    manager.resolveFlag(world, x, y, z, Flags.HEAL_MIN_HEALTH, id),
                    manager.resolveFlag(world, x, y, z, Flags.HEAL_MAX_HEALTH, id));
        }

        Optional<Double> feedAmount = manager.resolveFlagIfSet(world, x, y, z, Flags.FEED_AMOUNT, id);
        if (feedAmount.isPresent()
                && elapsed(this.lastFeed, id, delay(manager, world, x, y, z, Flags.FEED_DELAY, id), now)) {
            applyFeed(player, feedAmount.get(),
                    manager.resolveFlag(world, x, y, z, Flags.FEED_MIN_HUNGER, id),
                    manager.resolveFlag(world, x, y, z, Flags.FEED_MAX_HUNGER, id));
        }
    }

    private static double delay(RegionManager manager, String world, double x, double y, double z,
                                Flag<Double> flag, UUID id) {
        return Math.max(MIN_DELAY_SECONDS, manager.resolveFlag(world, x, y, z, flag, id));
    }

    private static boolean elapsed(Map<UUID, Long> last, UUID id, double delaySeconds, long now) {
        long delayMillis = (long) (delaySeconds * 1000);
        long previous = last.getOrDefault(id, 0L);
        if (now - previous >= delayMillis) {
            last.put(id, now);
            return true;
        }
        return false;
    }

    private static void applyHeal(RegionPlayer player, double amount, double min, double max) {
        double current = player.getHealth();
        double ceiling = Math.min(max, player.getMaxHealth());
        if (amount > 0 && current >= ceiling) {
            return;
        }
        if (amount < 0 && current <= min) {
            return;
        }
        double next = Math.max(min, Math.min(current + amount, ceiling));
        player.setHealth(Math.max(0.0, Math.min(next, player.getMaxHealth())));
    }

    private static void applyFeed(RegionPlayer player, double amount, double min, double max) {
        int current = player.getFoodLevel();
        int floor = (int) Math.max(0, min);
        int ceiling = (int) Math.min(20, max);
        if (amount > 0 && current >= ceiling) {
            return;
        }
        if (amount < 0 && current <= floor) {
            return;
        }
        int next = (int) Math.round(current + amount);
        player.setFoodLevel(Math.max(floor, Math.min(next, ceiling)));
    }
}
