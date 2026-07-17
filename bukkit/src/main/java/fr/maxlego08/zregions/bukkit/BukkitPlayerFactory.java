package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayerFactory;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * Wraps Bukkit {@link Player}s into platform-agnostic RegionPlayers, exposing
 * the position/world data the region engine needs.
 */
public final class BukkitPlayerFactory extends RegionPlayerFactory<Player> {

    private final ZRegionsPlugin plugin;
    private final BukkitAudiences audiences;

    /** The parsed border particle, cached per config value (parsed once, not per point). */
    private volatile String cachedParticleName;
    private volatile Particle cachedParticle = Particle.FLAME;

    public BukkitPlayerFactory(ZRegionsPlugin plugin, BukkitAudiences audiences) {
        this.plugin = plugin;
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

    /** {@link Player#spawnParticle} sends per-player packets — nobody else sees the outline. */
    @Override
    protected void spawnBorderParticle(Player player, double x, double y, double z) {
        player.spawnParticle(resolveParticle(), x, y, z, 1, 0, 0, 0, 0);
    }

    private Particle resolveParticle() {
        String name = this.plugin.getConfiguration().getBorderParticle();
        if (!name.equals(this.cachedParticleName)) {
            Particle parsed;
            try {
                parsed = Particle.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                this.plugin.getLogger().warn("Unknown border particle '" + name + "', using FLAME.");
                parsed = Particle.FLAME;
            }
            this.cachedParticle = parsed;
            this.cachedParticleName = name;
        }
        return this.cachedParticle;
    }

    @Override
    protected boolean isOnline(Player player) {
        return player.isOnline();
    }
}
