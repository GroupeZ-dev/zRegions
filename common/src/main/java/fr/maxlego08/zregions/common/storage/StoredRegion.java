package fr.maxlego08.zregions.common.storage;

import java.util.List;
import java.util.UUID;

/**
 * A raw storage snapshot of a region and its children rows. The region manager
 * converts these to live {@code ZRegion} instances via the shape codec.
 * Multi-server-ready: carries origin_server and the optimistic-lock version.
 */
public record StoredRegion(
        UUID id,
        String name,
        String world,
        int priority,
        String shapeType,
        String shapeData,
        UUID parentId,
        boolean global,
        String originServer,
        int version,
        List<StoredFlag> flags,
        List<StoredMember> members
) {

    public record StoredFlag(String flagKey, String groupTarget, String value) {
    }

    public record StoredMember(UUID playerId, String role) {
    }
}
