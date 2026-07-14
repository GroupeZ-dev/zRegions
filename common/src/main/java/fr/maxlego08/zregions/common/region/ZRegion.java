package fr.maxlego08.zregions.common.region;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.region.RegionMember;
import fr.maxlego08.zregions.api.shape.RegionShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live, mutable region held in the manager's cache. Thread-safe: members and
 * flag values live in concurrent maps, priority is volatile. Mutations must go
 * through {@link ZRegionManager} so persistence and the spatial index stay in sync.
 * Equality is by id, which lets a reloaded instance replace an old one in the index.
 */
public final class ZRegion implements Region {

    private final UUID id;
    private final String name;
    private final String worldName;
    private final RegionShape shape;
    private final UUID parentId;
    private final boolean global;
    private volatile int priority;
    private final ConcurrentHashMap<UUID, MemberRole> members = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<GroupTarget, Object>> flagValues = new ConcurrentHashMap<>();

    public ZRegion(UUID id, String name, String worldName, RegionShape shape, int priority, UUID parentId, boolean global) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.shape = global ? shape : Objects.requireNonNull(shape, "shape");
        this.priority = priority;
        this.parentId = parentId;
        this.global = global;
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
    public String getWorldName() {
        return this.worldName;
    }

    @Override
    public RegionShape getShape() {
        return this.shape;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public Optional<UUID> getParentId() {
        return Optional.ofNullable(this.parentId);
    }

    @Override
    public boolean isGlobal() {
        return this.global;
    }

    @Override
    public Collection<RegionMember> getMembers() {
        List<RegionMember> list = new ArrayList<>(this.members.size());
        this.members.forEach((playerId, role) -> list.add(new RegionMember(playerId, role)));
        return list;
    }

    @Override
    public boolean hasRole(UUID playerId, MemberRole role) {
        return playerId != null && this.members.get(playerId) == role;
    }

    @Override
    public GroupTarget targetFor(UUID playerId) {
        if (playerId == null) return GroupTarget.VISITOR;
        MemberRole role = this.members.get(playerId);
        if (role == MemberRole.OWNER) return GroupTarget.OWNER;
        if (role == MemberRole.MEMBER) return GroupTarget.MEMBER;
        return GroupTarget.VISITOR;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getFlag(Flag<T> flag, GroupTarget target) {
        Map<GroupTarget, Object> values = this.flagValues.get(flag.getKey());
        return values == null ? Optional.empty() : Optional.ofNullable((T) values.get(target));
    }

    // --- mutators, called by the manager only ---

    public void setFlagValue(String flagKey, GroupTarget target, Object value) {
        this.flagValues.computeIfAbsent(flagKey, key -> new ConcurrentHashMap<>()).put(target, value);
    }

    public void removeFlagValue(String flagKey, GroupTarget target) {
        this.flagValues.computeIfPresent(flagKey, (key, values) -> {
            values.remove(target);
            return values.isEmpty() ? null : values;
        });
    }

    public void putMember(UUID playerId, MemberRole role) {
        this.members.put(playerId, role);
    }

    public void removeMember(UUID playerId) {
        this.members.remove(playerId);
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    /** A defensive snapshot of the raw flag values, used by the manager for persistence. */
    public Map<String, Map<GroupTarget, Object>> flagValuesSnapshot() {
        Map<String, Map<GroupTarget, Object>> snapshot = new HashMap<>();
        this.flagValues.forEach((key, values) -> snapshot.put(key, new HashMap<>(values)));
        return snapshot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZRegion other)) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "ZRegion{" + this.name + " in " + this.worldName + ", priority=" + this.priority + "}";
    }
}
