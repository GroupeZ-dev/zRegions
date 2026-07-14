package fr.maxlego08.zregions.common.spatial;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.region.RegionMember;
import fr.maxlego08.zregions.api.shape.RegionShape;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal {@link Region} stub for index tests: only the shape matters, everything
 * else is unsupported.
 */
final class FakeRegion implements Region {

    private final UUID id = UUID.randomUUID();
    private final String name;
    private final RegionShape shape;

    FakeRegion(String name, RegionShape shape) {
        this.name = name;
        this.shape = shape;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RegionShape getShape() {
        return this.shape;
    }

    @Override
    public String getWorldName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPriority() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<UUID> getParentId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isGlobal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<RegionMember> getMembers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasRole(UUID playerId, MemberRole role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GroupTarget targetFor(UUID playerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> getFlag(Flag<T> flag, GroupTarget target) {
        throw new UnsupportedOperationException();
    }
}
