package fr.maxlego08.zregions.api.region;

import java.util.Objects;
import java.util.UUID;

/**
 * A player attached to a region with a role. Immutable.
 */
public final class RegionMember {

    private final UUID playerId;
    private final MemberRole role;

    public RegionMember(UUID playerId, MemberRole role) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.role = Objects.requireNonNull(role, "role");
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public MemberRole getRole() {
        return this.role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionMember other)) return false;
        return this.playerId.equals(other.playerId) && this.role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.playerId, this.role);
    }
}
