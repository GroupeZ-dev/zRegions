package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Region-scoped respawn point: the {@code spawn} location flag of the region a
 * player died in becomes their respawn location. {@code PlayerRespawnEvent} no
 * longer carries the death position, so it is captured on death and looked up on
 * respawn.
 *
 * <p>The respawn is set at {@code HIGH} priority — before {@link MovementListener}'s
 * {@code MONITOR} handler, so the tracker still observes the final location, and
 * after most spawn plugins have had their say (a region spawn is an explicit
 * admin choice and deliberately wins).</p>
 */
public final class RespawnListener implements Listener {

    private final ZRegionsBukkitPlugin plugin;
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    public RespawnListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        this.deathLocations.put(player.getUniqueId(), player.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Location death = this.deathLocations.remove(playerId);
        if (death == null || death.getWorld() == null) {
            return;
        }
        Optional<RegionLocation> spawn = this.plugin.getRegionManager().resolveFlagIfSet(
                death.getWorld().getName(), death.getX(), death.getY(), death.getZ(),
                Flags.SPAWN, playerId);
        if (spawn.isEmpty()) {
            return;
        }
        RegionLocation target = spawn.get();
        World world = Bukkit.getWorld(target.getWorldName());
        if (world == null) {
            return;
        }
        event.setRespawnLocation(new Location(world, target.getX(), target.getY(), target.getZ(),
                target.getYaw(), target.getPitch()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.deathLocations.remove(event.getPlayer().getUniqueId());
    }
}
