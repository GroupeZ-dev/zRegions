package fr.maxlego08.zregions.common.movement;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which regions every online player is inside and computes the enter/exit
 * diff on movement (plan §9: recomputed only when a block boundary is crossed —
 * the platform listener does that cheap pre-filter). All the decisions (entry/exit
 * flags, greeting/farewell) are made here in common; platform listeners only
 * translate events and apply the verdict.
 *
 * <p>Enforcement and state are deliberately split: {@link #checkMove} only judges
 * (called early, so the platform can cancel), {@link #commitMove} only records and
 * messages (called once the outcome of the native event is final — on Bukkit, from
 * a MONITOR-priority handler). {@link #handleMove} combines both for callers that
 * have no two-phase event model.</p>
 *
 * <p>The tracker stores region <em>ids</em>, never instances: a region deleted or
 * copy-swapped (redefine/setparent) while players stand inside must not linger —
 * ids are re-resolved against the manager on every diff and silently dropped when
 * they no longer exist (no EXIT enforcement for vanished regions).</p>
 *
 * <p>Thread-safety: a given player's events run sequentially (on Folia, on that
 * player's region thread), and the maps are concurrent — no extra locking.</p>
 */
public final class RegionMovementTracker {

    private final ZRegionsPlugin plugin;
    private final Map<UUID, Set<UUID>> currentRegions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDeniedMessage = new ConcurrentHashMap<>();

    public RegionMovementTracker(ZRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Judges a move (walk or teleport) to {@code to} WITHOUT touching the tracked
     * state. Returns {@code false} when the move must be refused: an entered
     * region denies {@code entry}, or a left region denies {@code exit}. Each
     * region's border is enforced independently, parents included (deliberately
     * region-scoped, not positional — a border is a property of its region).
     *
     * @param bypass skips enforcement entirely (admin bypass permission)
     */
    public boolean checkMove(RegionPlayer player, RegionLocation to, boolean bypass) {
        if (bypass) {
            return true;
        }
        UUID playerId = player.getUniqueId();
        RegionManager manager = this.plugin.getRegionManager();
        List<Region> now = regionsAt(to);
        Set<UUID> was = this.currentRegions.getOrDefault(playerId, Set.of());

        // entering regions, highest priority first (getRegionsAt is sorted)
        for (Region region : now) {
            if (!was.contains(region.getId()) && !manager.resolveFlag(region, Flags.ENTRY, playerId)) {
                sendBorderDenied(player, region, Flags.ENTRY_DENY_MESSAGE, Message.ENTRY_DENIED);
                return false;
            }
        }
        Set<UUID> nowIds = idsOf(now);
        for (UUID regionId : was) {
            if (nowIds.contains(regionId)) {
                continue;
            }
            Region region = manager.getRegion(regionId).orElse(null);
            if (region != null && !manager.resolveFlag(region, Flags.EXIT, playerId)) {
                sendBorderDenied(player, region, Flags.EXIT_DENY_MESSAGE, Message.EXIT_DENIED);
                return false;
            }
        }
        return true;
    }

    /**
     * Records the player's final position and sends farewell/greeting messages.
     * No enforcement — call it only once the native move/teleport is known to
     * happen (Bukkit: MONITOR priority, ignoreCancelled).
     */
    public void commitMove(RegionPlayer player, RegionLocation to) {
        applyPosition(player, to, true);
    }

    /** {@link #checkMove} + {@link #commitMove} for single-phase callers (and tests). */
    public boolean handleMove(RegionPlayer player, RegionLocation to, boolean bypass) {
        if (!checkMove(player, to, bypass)) {
            return false;
        }
        commitMove(player, to);
        return true;
    }

    /**
     * (Re)initializes the tracked set at {@code location} without any enforcement —
     * join, respawn and world change. Farewells are sent for the regions left
     * behind (dying in a farewell region says goodbye) and greetings for the
     * regions arrived in (a join inside a shop region greets).
     */
    public void handleArrival(RegionPlayer player, RegionLocation location) {
        applyPosition(player, location, true);
    }

    /** Initializes the tracked set silently — enable-time seeding of online players. */
    public void seed(RegionPlayer player, RegionLocation location) {
        applyPosition(player, location, false);
    }

    /** Purges the per-player state on disconnect. */
    public void handleQuit(UUID playerId) {
        this.currentRegions.remove(playerId);
        this.lastDeniedMessage.remove(playerId);
    }

    /** The ids of the regions the player is currently tracked in (empty when unknown). */
    public Set<UUID> getRegionIds(UUID playerId) {
        return this.currentRegions.getOrDefault(playerId, Set.of());
    }

    // --- internals ---

    private void applyPosition(RegionPlayer player, RegionLocation location, boolean withMessages) {
        UUID playerId = player.getUniqueId();
        List<Region> now = regionsAt(location);
        Set<UUID> nowIds = idsOf(now);
        Set<UUID> was = this.currentRegions.get(playerId);
        if (nowIds.equals(was)) {
            return;
        }
        this.currentRegions.put(playerId, nowIds);
        if (!withMessages) {
            return;
        }

        if (was != null) {
            RegionManager manager = this.plugin.getRegionManager();
            List<Region> left = new ArrayList<>();
            for (UUID regionId : was) {
                if (!nowIds.contains(regionId)) {
                    manager.getRegion(regionId).ifPresent(region -> {
                        sendZoneMessage(player, region, Flags.FAREWELL);
                        left.add(region);
                    });
                }
            }
            if (!left.isEmpty()) {
                left.sort(Comparator.comparingInt(Region::getPriority).reversed());
                sendExitDisplays(player, left);
            }
        }
        List<Region> entered = new ArrayList<>();
        for (Region region : now) {
            if (was == null || !was.contains(region.getId())) {
                sendZoneMessage(player, region, Flags.GREETING);
                entered.add(region);
            }
        }
        if (!entered.isEmpty()) {
            sendZoneDisplays(player, entered);
        }
    }

    private List<Region> regionsAt(RegionLocation location) {
        return this.plugin.getRegionManager()
                .getRegionsAt(location.getWorldName(), location.getX(), location.getY(), location.getZ());
    }

    private static Set<UUID> idsOf(List<Region> regions) {
        if (regions.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>(regions.size());
        for (Region region : regions) {
            ids.add(region.getId());
        }
        return ids;
    }

    private void sendZoneMessage(RegionPlayer player, Region region, Flag<String> flag) {
        String text = this.plugin.getRegionManager().resolveFlag(region, flag, player.getUniqueId());
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(render(player, region, text));
    }

    /**
     * Title/subtitle and action bar on enter — same flag model as greeting, but
     * displays are last-write-wins on the client: only the FIRST region defining
     * each channel shows ({@code entered} comes in priority order, highest first),
     * so the highest-priority display wins instead of being overwritten.
     */
    private void sendZoneDisplays(RegionPlayer player, List<Region> entered) {
        RegionManager manager = this.plugin.getRegionManager();
        UUID playerId = player.getUniqueId();

        boolean titleShown = false;
        boolean actionBarShown = false;
        for (Region region : entered) {
            if (!titleShown) {
                String title = manager.resolveFlag(region, Flags.TITLE, playerId);
                String subtitle = manager.resolveFlag(region, Flags.SUBTITLE, playerId);
                if (!isEmpty(title) || !isEmpty(subtitle)) {
                    player.sendTitle(
                            isEmpty(title) ? Component.empty() : render(player, region, title),
                            isEmpty(subtitle) ? Component.empty() : render(player, region, subtitle));
                    titleShown = true;
                }
            }
            if (!actionBarShown) {
                String actionBar = manager.resolveFlag(region, Flags.ACTION_BAR, playerId);
                if (!isEmpty(actionBar)) {
                    player.sendActionBar(render(player, region, actionBar));
                    actionBarShown = true;
                }
            }
            if (titleShown && actionBarShown) {
                return;
            }
        }
    }

    private Component render(RegionPlayer player, Region region, String text) {
        return this.plugin.getMessages().formatRaw(text,
                "player", player.getName(),
                "region", region.getName());
    }

    private static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * farewell-title/subtitle on exit — the highest-priority left region that defines
     * either channel wins (client displays are last-write-wins, {@code left} is sorted
     * by priority descending), mirroring the enter displays.
     */
    private void sendExitDisplays(RegionPlayer player, List<Region> left) {
        RegionManager manager = this.plugin.getRegionManager();
        UUID playerId = player.getUniqueId();
        for (Region region : left) {
            String title = manager.resolveFlag(region, Flags.FAREWELL_TITLE, playerId);
            String subtitle = manager.resolveFlag(region, Flags.FAREWELL_SUBTITLE, playerId);
            if (!isEmpty(title) || !isEmpty(subtitle)) {
                player.sendTitle(
                        isEmpty(title) ? Component.empty() : render(player, region, title),
                        isEmpty(subtitle) ? Component.empty() : render(player, region, subtitle));
                return;
            }
        }
    }

    /**
     * Border denial: a region may override the generic {@code fallback} with a custom
     * {@code entry-deny-message}/{@code exit-deny-message} (region-scoped, MiniMessage).
     */
    private void sendBorderDenied(RegionPlayer player, Region region, Flag<String> messageFlag, Message fallback) {
        long now = System.currentTimeMillis();
        Long last = this.lastDeniedMessage.get(player.getUniqueId());
        if (last != null && now - last < this.plugin.getConfiguration().getDenyMessageThrottleMillis()) {
            return;
        }
        this.lastDeniedMessage.put(player.getUniqueId(), now);
        String custom = this.plugin.getRegionManager().resolveFlag(region, messageFlag, player.getUniqueId());
        if (custom != null && !custom.isEmpty()) {
            player.sendMessage(render(player, region, custom));
        } else {
            this.plugin.getMessages().send(player, fallback, "region", region.getName());
        }
    }
}
