package fr.maxlego08.zregions.common.storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage contract. All methods are synchronous — callers schedule them on the
 * async pool (LuckPerms' StorageImplementation model). The database is the shared
 * source of truth in multi-server mode; it is NEVER read on the protection hot path.
 */
public interface RegionStorage {

    void connect() throws Exception;

    void disconnect();

    /** Loads the regions owned by {@code serverName} plus the "global" ones. */
    List<StoredRegion> loadRegions(String serverName);

    Optional<StoredRegion> loadRegion(UUID id);

    /** Inserts or fully updates a region row (not its flags/members). */
    void saveRegion(StoredRegion region);

    /** Deletes the region row; flags/members follow via ON DELETE CASCADE. */
    void deleteRegion(UUID id);

    void saveFlag(UUID regionId, String flagKey, String groupTarget, String value);

    void deleteFlag(UUID regionId, String flagKey, String groupTarget);

    void saveMember(UUID regionId, UUID playerId, String role);

    void deleteMember(UUID regionId, UUID playerId);
}
