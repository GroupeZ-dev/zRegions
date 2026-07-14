package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayerFactory;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Wraps Bukkit {@link Player}s into platform-agnostic RegionPlayers, exposing
 * the position/world data the region engine needs.
 */
public final class BukkitPlayerFactory extends RegionPlayerFactory<Player> {

    private final BukkitAudiences audiences;

    public BukkitPlayerFactory(BukkitAudiences audiences) {
        this.audiences = audiences;
    }

    @Override
    protected UUID getUniqueId(Player player) {
        return player.getUniqueId();
    }

    @Override
    protected String getName(Player player) {
        return player.getName();
    }

    @Override
    protected RegionLocation getLocation(Player player) {
        Location location = player.getLocation();
        return new RegionLocation(player.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    @Override
    protected boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    @Override
    protected void sendMessage(Player player, Component message) {
        this.audiences.sender(player).sendMessage(message);
    }

    @Override
    protected void teleport(Player player, RegionLocation location) {
        World world = Bukkit.getWorld(location.getWorldName());
        if (world == null) {
            Bukkit.getLogger().warning("[zRegions] Unable to teleport " + player.getName()
                    + ": world " + location.getWorldName() + " is not loaded.");
            return;
        }
        player.teleport(new Location(world, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()));
    }

    @Override
    protected boolean isOnline(Player player) {
        return player.isOnline();
    }
}
