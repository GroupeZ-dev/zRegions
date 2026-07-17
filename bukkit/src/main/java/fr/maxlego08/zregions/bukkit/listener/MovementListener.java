package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.movement.RegionMovementTracker;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges the native movement events to the common {@link RegionMovementTracker}.
 * The tracker owns every decision and every entry/exit/greeting/farewell message —
 * this listener only converts, pre-filters (same-block moves) and applies verdicts.
 *
 * <p>Two-phase pattern per event: enforcement runs at NORMAL priority (checkMove →
 * cancel/push-back), while the tracked state is committed at MONITOR priority with
 * ignoreCancelled, once the final outcome and destination are known — a plugin
 * cancelling or rewriting the event at a later priority can no longer desync the
 * tracker.</p>
 *
 * <p>1.20.4 dispatch facts (verified against the jar): PlayerTeleportEvent AND
 * PlayerPortalEvent each declare their own HandlerList, so move handlers never
 * see teleports, teleport handlers never see portals — each needs its own pair.</p>
 */
public final class MovementListener implements Listener {

    private final ZRegionsBukkitPlugin plugin;
    private final Map<UUID, Long> lastDeniedMessage = new ConcurrentHashMap<>();

    public MovementListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    // --- walking ---

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to)) {
            return;
        }

        Player nativePlayer = event.getPlayer();
        RegionPlayer player = this.plugin.getPlayerFactory().wrap(nativePlayer);
        boolean bypass = nativePlayer.hasPermission(this.plugin.getConfiguration().getBypassPermission());
        if (!this.plugin.getMovementTracker().checkMove(player, toRegionLocation(to), bypass)) {
            // Push back instead of cancelling — setCancelled glitches on some clients.
            event.setTo(from);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMoveMonitor(PlayerMoveEvent event) {
        commit(event.getPlayer(), event.getFrom(), event.getTo());
    }

    // --- teleports (own HandlerList — not delivered to the move handlers) ---

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Player nativePlayer = event.getPlayer();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && isDenied(nativePlayer, Flags.ENDERPEARL, to)) {
            event.setCancelled(true);
            sendDeniedMessage(nativePlayer);
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
                && isDenied(nativePlayer, Flags.CHORUS_FRUIT, to)) {
            event.setCancelled(true);
            sendDeniedMessage(nativePlayer);
            return;
        }

        if (!checkArrivalAt(nativePlayer, to)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportMonitor(PlayerTeleportEvent event) {
        commit(event.getPlayer(), event.getFrom(), event.getTo());
    }

    // --- portals (own HandlerList again — bypass the teleport handlers entirely) ---

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Location to = event.getTo();
        if (to != null && !checkArrivalAt(event.getPlayer(), to)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalMonitor(PlayerPortalEvent event) {
        commit(event.getPlayer(), event.getFrom(), event.getTo());
    }

    // --- lifecycle ---

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        RegionPlayer player = this.plugin.getPlayerFactory().wrap(event.getPlayer());
        this.plugin.getMovementTracker().handleArrival(player, player.getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.plugin.getMovementTracker().handleQuit(playerId);
        this.lastDeniedMessage.remove(playerId);
    }

    /** MONITOR: spawn plugins rewrite the respawn location at lower priorities. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        RegionPlayer player = this.plugin.getPlayerFactory().wrap(event.getPlayer());
        this.plugin.getMovementTracker().handleArrival(player, toRegionLocation(event.getRespawnLocation()));
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        RegionPlayer player = this.plugin.getPlayerFactory().wrap(event.getPlayer());
        this.plugin.getMovementTracker().handleArrival(player, player.getLocation());
    }

    // --- helpers ---

    private boolean checkArrivalAt(Player nativePlayer, Location to) {
        RegionPlayer player = this.plugin.getPlayerFactory().wrap(nativePlayer);
        boolean bypass = nativePlayer.hasPermission(this.plugin.getConfiguration().getBypassPermission());
        return this.plugin.getMovementTracker().checkMove(player, toRegionLocation(to), bypass);
    }

    /** Commits the final destination to the tracker (state + greeting/farewell). */
    private void commit(Player nativePlayer, Location from, Location to) {
        if (to == null || sameBlock(from, to)) {
            return;
        }
        RegionPlayer player = this.plugin.getPlayerFactory().wrap(nativePlayer);
        this.plugin.getMovementTracker().commitMove(player, toRegionLocation(to));
    }

    private static boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ() && from.getWorld().equals(to.getWorld());
    }

    private RegionLocation toRegionLocation(Location location) {
        return new RegionLocation(location.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    private boolean isDenied(Player player, Flag<Boolean> flag, Location location) {
        if (player.hasPermission(this.plugin.getConfiguration().getBypassPermission())) return false;
        boolean allowed = this.plugin.getRegionManager().resolveFlag(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), flag, player.getUniqueId());
        return !allowed;
    }

    private void sendDeniedMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = this.lastDeniedMessage.get(player.getUniqueId());
        if (last != null && now - last < this.plugin.getConfiguration().getDenyMessageThrottleMillis()) return;
        this.lastDeniedMessage.put(player.getUniqueId(), now);
        this.plugin.getMessages().send(this.plugin.getPlayerFactory().wrap(player), Message.ACTION_DENIED);
    }
}
