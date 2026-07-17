package fr.maxlego08.zregions.api.manager;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.MemberRole;
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

    /**
     * The effective value of {@code flag} for a player in this specific region
     * (region, then its parent chain, then the flag default — no positional lookup,
     * no global fallback). Used for region-scoped checks such as entry/exit.
     */
    <T> T resolveFlag(Region region, Flag<T> flag, UUID playerId);

    /** Sets a flag value on the region (cache first), then persists it asynchronously. */
    <T> void setFlag(Region region, Flag<T> flag, GroupTarget target, T value);

    /** Removes a flag value from the region (cache first), then persists asynchronously. */
    void removeFlag(Region region, Flag<?> flag, GroupTarget target);

    /** Adds or updates a member role (cache first), then persists asynchronously. */
    void setMember(Region region, UUID playerId, MemberRole role);

    void removeMember(Region region, UUID playerId);

    void setPriority(Region region, int priority);

    /**
     * Sets (or clears, with {@code null}) the parent used for flag inheritance.
     * Returns the updated live instance — the passed-in one is stale afterwards.
     *
     * @throws IllegalArgumentException if this would create a parent cycle
     */
    Region setParent(Region region, Region parent);

    /**
     * Replaces the region's shape and re-indexes it. Returns the updated live
     * instance — the passed-in one is stale afterwards.
     *
     * @throws IllegalArgumentException for the per-world global region (no shape)
     */
    Region redefine(Region region, RegionShape shape);

    Collection<Region> getRegions(String worldName);

    Collection<Region> getRegions();

    /**
     * Reloads a single region from storage and re-indexes it (removed if absent).
     * The single anchor point where cross-server messaging plugs in.
     */
    CompletableFuture<Void> reload(UUID regionId);
}
