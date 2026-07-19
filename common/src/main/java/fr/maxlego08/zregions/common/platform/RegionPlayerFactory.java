package fr.maxlego08.zregions.common.platform;

import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.Optional;
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

    /** Shows a message in the player's action bar. */
    protected abstract void sendActionBar(T handle, Component message);

    /** Shows a title/subtitle pair with the platform's default fade timings. */
    protected abstract void sendTitle(T handle, Component title, Component subtitle);

    protected abstract void teleport(T handle, RegionLocation location);

    /**
     * Finds a safe standable spot at {@code target}'s column. Reads the world —
     * call on the game thread. Empty when the world is unloaded or nothing is safe.
     */
    protected abstract Optional<RegionLocation> findSafeSpot(RegionLocation target);

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
