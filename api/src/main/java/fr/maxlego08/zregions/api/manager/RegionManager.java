package fr.maxlego08.zregions.api.manager;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.RegionShape;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Central region service. On Bukkit this is exposed through the ServicesManager.
 *
 * <p>All queries are served from an in-memory chunk index — they never touch the
 * database. Mutations update the cache first, then persist asynchronously.</p>
 */
public interface RegionManager {

    /**
     * Creates, indexes and asynchronously persists a new region.
     *
     * @throws IllegalArgumentException if a region with this name already exists in the world
     */
    Region createRegion(String worldName, String name, RegionShape shape, int priority, UUID creator);

    /** Removes the region from the cache/index and asynchronously deletes it from storage. */
    void deleteRegion(Region region);

    Optional<Region> getRegion(String worldName, String name);

    Optional<Region> getRegion(UUID regionId);

    /** All regions containing this point, highest priority first. HOT PATH. */
    List<Region> getRegionsAt(String worldName, double x, double y, double z);

    Optional<Region> getHighestRegionAt(String worldName, double x, double y, double z);

    /**
     * The effective value of {@code flag} at a location for a player
     * (priority walk, then parent, then per-world global region, then flag default).
     * {@code playerId} may be null (environment events). HOT PATH.
     */
    <T> T resolveFlag(String worldName, double x, double y, double z, Flag<T> flag, UUID playerId);

    Collection<Region> getRegions(String worldName);

    Collection<Region> getRegions();

    /**
     * Reloads a single region from storage and re-indexes it (removed if absent).
     * The single anchor point where cross-server messaging plugs in.
     */
    CompletableFuture<Void> reload(UUID regionId);
}
