package fr.maxlego08.zregions.api.region;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.shape.RegionShape;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * A protected region: a shape in a world, with a priority, members and flag values.
 *
 * <p>Regions belong to exactly one {@code (server, world)} pair. Effective flag
 * resolution (priority walk, parent inheritance, global fallback) is performed by
 * the {@link fr.maxlego08.zregions.api.manager.RegionManager}, not by the region itself.</p>
 */
public interface Region {

    UUID getId();

    String getName();

    String getWorldName();

    RegionShape getShape();

    /** Higher priority wins when regions overlap (WorldGuard semantics). */
    int getPriority();

    Optional<UUID> getParentId();

    /** True for the per-world {@code __global__} region. */
    boolean isGlobal();

    Collection<RegionMember> getMembers();

    boolean hasRole(UUID playerId, MemberRole role);

    /**
     * The most specific target this player matches in this region:
     * OWNER if owner, MEMBER if member, VISITOR otherwise (or for {@code null}).
     */
    GroupTarget targetFor(UUID playerId);

    /**
     * The raw value this region defines for {@code flag} and {@code target}
     * — no inheritance, no default. Empty when the region does not set it.
     */
    <T> Optional<T> getFlag(Flag<T> flag, GroupTarget target);
}
