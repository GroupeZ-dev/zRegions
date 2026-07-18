package fr.maxlego08.zregions.common.platform;

import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Produces {@link RegionPlayer} wrappers from the platform's native player type
 * (same pattern as the sender factory / LuckPerms' SenderFactory).
 *
 * @param <T> the native player type (org.bukkit.entity.Player on Bukkit)
 */
public abstract class RegionPlayerFactory<T> {

    protected abstract UUID getUniqueId(T handle);

    protected abstract String getName(T handle);

    protected abstract RegionLocation getLocation(T handle);

    protected abstract boolean hasPermission(T handle, String permission);

    protected abstract void sendMessage(T handle, Component message);

    protected abstract void teleport(T handle, RegionLocation location);

    /** Spawns a border particle at the coordinates, visible to this player only. */
    protected abstract void spawnBorderParticle(T handle, double x, double y, double z);

    /** Hands the player the selection wand item. */
    protected abstract void giveWand(T handle);

    protected abstract boolean isOnline(T handle);

    public final RegionPlayer wrap(T handle) {
        Objects.requireNonNull(handle, "handle");
        return new AbstractRegionPlayer<>(this, handle);
    }
}
