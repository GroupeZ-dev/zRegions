package fr.maxlego08.zregions.common.region;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerAdapter;
import fr.maxlego08.zregions.common.shape.ShapeCodec;
import fr.maxlego08.zregions.common.spatial.ChunkRegionIndex;
import fr.maxlego08.zregions.common.storage.RegionStorage;
import fr.maxlego08.zregions.common.storage.StoredRegion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The in-memory region service: cache, per-world chunk index and effective flag
 * resolution. Queries never touch the database; mutations update the cache under
 * a single write lock (the index requires it), then persist asynchronously.
 */
public class ZRegionManager implements RegionManager {

    private static final int MAX_PARENT_HOPS = 10;

    private final ZRegionsPlugin plugin;
    private final Map<UUID, ZRegion> byId = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<String, ZRegion>> byName = new ConcurrentHashMap<>();
    private final Map<String, ChunkRegionIndex> indices = new ConcurrentHashMap<>();
    private final Map<String, ZRegion> globalByWorld = new ConcurrentHashMap<>();
    private final ReentrantLock writeLock = new ReentrantLock();

    public ZRegionManager(ZRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads every region of this server from storage into the cache and index.
     * Called once during enable(). A corrupted region is logged and skipped, it
     * never prevents the plugin from booting.
     */
    public void loadAllBlocking() {
        List<StoredRegion> storedRegions = this.plugin.getStorage().loadRegions(this.plugin.getConfiguration().getServerName());
        this.writeLock.lock();
        try {
            for (StoredRegion stored : storedRegions) {
                try {
                    addToCaches(toLiveRegion(stored));
                } catch (Exception exception) {
                    this.plugin.getLogger().warn("Skipping corrupted region '" + stored.name() + "' (" + stored.id() + ")", exception);
                }
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public Region createRegion(String worldName, String name, RegionShape shape, int priority, UUID creator) {
        ZRegion region = new ZRegion(UUID.randomUUID(), name, worldName, shape, priority, null, false);
        if (creator != null) {
            region.putMember(creator, MemberRole.OWNER);
        }

        this.writeLock.lock();
        try {
            if (getRegion(worldName, name).isPresent()) {
                throw new IllegalArgumentException("A region named '" + name + "' already exists in world '" + worldName + "'");
            }
            addToCaches(region);
        } finally {
            this.writeLock.unlock();
        }

        StoredRegion stored = toStored(region);
        scheduler().executeAsync(() -> {
            RegionStorage storage = this.plugin.getStorage();
            storage.saveRegion(stored);
            if (creator != null) {
                storage.saveMember(region.getId(), creator, MemberRole.OWNER.name());
            }
        });
        return region;
    }

    @Override
    public void deleteRegion(Region region) {
        this.writeLock.lock();
        try {
            removeFromCaches((ZRegion) region);
        } finally {
            this.writeLock.unlock();
        }
        scheduler().executeAsync(() -> this.plugin.getStorage().deleteRegion(region.getId()));
    }

    @Override
    public Optional<Region> getRegion(String worldName, String name) {
        Map<String, ZRegion> names = this.byName.get(worldName);
        return names == null ? Optional.empty() : Optional.ofNullable(names.get(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<Region> getRegion(UUID regionId) {
        return Optional.ofNullable(this.byId.get(regionId));
    }

    @Override
    public List<Region> getRegionsAt(String worldName, double x, double y, double z) {
        ChunkRegionIndex index = this.indices.get(worldName);
        if (index == null) return List.of();
        List<Region> regions = index.query(x, y, z);
        if (regions.size() <= 1) return regions;
        List<Region> sorted = new ArrayList<>(regions);
        sorted.sort(Comparator.comparingInt(Region::getPriority).reversed());
        return sorted;
    }

    @Override
    public Optional<Region> getHighestRegionAt(String worldName, double x, double y, double z) {
        ChunkRegionIndex index = this.indices.get(worldName);
        if (index == null) return Optional.empty();
        Region best = null;
        for (Region region : index.query(x, y, z)) {
            if (best == null || region.getPriority() > best.getPriority()) {
                best = region;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    public <T> T resolveFlag(String worldName, double x, double y, double z, Flag<T> flag, UUID playerId) {
        for (Region region : getRegionsAt(worldName, x, y, z)) {
            Optional<T> value = lookupWithParents(region, flag, playerId);
            if (value.isPresent()) return value.get();
        }
        ZRegion global = this.globalByWorld.get(worldName);
        if (global != null) {
            Optional<T> value = lookupWithParents(global, flag, playerId);
            if (value.isPresent()) return value.get();
        }
        return flag.getDefaultValue();
    }

    @Override
    public Collection<Region> getRegions(String worldName) {
        Map<String, ZRegion> names = this.byName.get(worldName);
        return names == null ? List.of() : List.copyOf(names.values());
    }

    @Override
    public Collection<Region> getRegions() {
        return List.copyOf(this.byId.values());
    }

    /** Sets a flag value on the region (cache first), then persists it asynchronously. */
    public <T> void setFlag(Region region, Flag<T> flag, GroupTarget target, T value) {
        ((ZRegion) region).setFlagValue(flag.getKey(), target, value);
        scheduler().executeAsync(() ->
                this.plugin.getStorage().saveFlag(region.getId(), flag.getKey(), target.name(), flag.serialize(value)));
    }

    /**
     * Reloads one region from storage and swaps it in the cache/index — removed
     * when the row no longer exists. THE multi-server anchor point: cross-server
     * messaging (v2) calls this on every region update ping, nothing else changes
     * (see ARCHITECTURE.md §14.8). The storage read runs on the async pool, the
     * index swap on the game/region thread.
     */
    @Override
    public CompletableFuture<Void> reload(UUID regionId) {
        SchedulerAdapter scheduler = scheduler();
        return CompletableFuture.runAsync(() -> {
            Optional<StoredRegion> stored = this.plugin.getStorage().loadRegion(regionId);
            scheduler.executeSync(() -> {
                this.writeLock.lock();
                try {
                    ZRegion old = this.byId.get(regionId);
                    if (old != null) {
                        removeFromCaches(old);
                    }
                    stored.ifPresent(storedRegion -> {
                        try {
                            addToCaches(toLiveRegion(storedRegion));
                        } catch (Exception exception) {
                            this.plugin.getLogger().warn("Unable to reload region " + regionId, exception);
                        }
                    });
                } finally {
                    this.writeLock.unlock();
                }
            });
        }, scheduler.async());
    }

    // --- conversion live <-> stored ---

    private ZRegion toLiveRegion(StoredRegion stored) {
        RegionShape shape = stored.shapeType() == null ? null
                : ShapeCodec.fromJson(ShapeType.valueOf(stored.shapeType()), stored.shapeData());
        ZRegion region = new ZRegion(stored.id(), stored.name(), stored.world(), shape,
                stored.priority(), stored.parentId(), stored.global());

        for (StoredRegion.StoredFlag storedFlag : stored.flags()) {
            Flag<?> flag = this.plugin.getFlagRegistry().getFlag(storedFlag.flagKey()).orElse(null);
            if (flag == null) {
                this.plugin.getLogger().warn("Region '" + stored.name() + "': unknown flag '" + storedFlag.flagKey() + "', skipping.");
                continue;
            }
            Optional<?> value = flag.parse(storedFlag.value());
            if (value.isEmpty()) {
                this.plugin.getLogger().warn("Region '" + stored.name() + "': invalid value '" + storedFlag.value()
                        + "' for flag '" + storedFlag.flagKey() + "', skipping.");
                continue;
            }
            region.setFlagValue(flag.getKey(), GroupTarget.parse(storedFlag.groupTarget(), GroupTarget.ALL), value.get());
        }

        for (StoredRegion.StoredMember member : stored.members()) {
            region.putMember(member.playerId(), MemberRole.parse(member.role(), MemberRole.MEMBER));
        }
        return region;
    }

    private StoredRegion toStored(ZRegion region) {
        List<StoredRegion.StoredFlag> flags = new ArrayList<>();
        region.flagValuesSnapshot().forEach((key, values) -> {
            Flag<?> flag = this.plugin.getFlagRegistry().getFlag(key).orElse(null);
            if (flag == null) return;
            values.forEach((target, value) -> flags.add(new StoredRegion.StoredFlag(key, target.name(), serialize(flag, value))));
        });

        List<StoredRegion.StoredMember> members = new ArrayList<>();
        region.getMembers().forEach(member -> members.add(new StoredRegion.StoredMember(member.getPlayerId(), member.getRole().name())));

        RegionShape shape = region.getShape();
        return new StoredRegion(region.getId(), region.getName(), region.getWorldName(), region.getPriority(),
                shape == null ? null : shape.getType().name(),
                shape == null ? null : ShapeCodec.toJson(shape),
                region.getParentId().orElse(null), region.isGlobal(),
                this.plugin.getConfiguration().getServerName(), 0, flags, members);
    }

    // --- internals ---

    /**
     * The value {@code region} defines for {@code flag}: most specific matching
     * target first, then ALL, then the parent chain (each parent re-evaluated with
     * its own targetFor). Bounded to {@value #MAX_PARENT_HOPS} hops to survive cycles.
     */
    private <T> Optional<T> lookupWithParents(Region region, Flag<T> flag, UUID playerId) {
        Region current = region;
        for (int hop = 0; current != null && hop <= MAX_PARENT_HOPS; hop++) {
            GroupTarget target = current.targetFor(playerId);
            Optional<T> value = current.getFlag(flag, target);
            if (value.isEmpty() && target != GroupTarget.ALL) {
                value = current.getFlag(flag, GroupTarget.ALL);
            }
            if (value.isPresent()) return value;
            UUID parentId = current.getParentId().orElse(null);
            current = parentId == null ? null : this.byId.get(parentId);
        }
        return Optional.empty();
    }

    /** Must be called under {@link #writeLock}. */
    private void addToCaches(ZRegion region) {
        this.byId.put(region.getId(), region);
        this.byName.computeIfAbsent(region.getWorldName(), world -> new ConcurrentHashMap<>())
                .put(region.getName().toLowerCase(Locale.ROOT), region);
        if (region.isGlobal()) {
            this.globalByWorld.put(region.getWorldName(), region);
        } else {
            this.indices.computeIfAbsent(region.getWorldName(), world -> new ChunkRegionIndex()).add(region);
        }
    }

    /** Must be called under {@link #writeLock}. */
    private void removeFromCaches(ZRegion region) {
        this.byId.remove(region.getId());
        ConcurrentHashMap<String, ZRegion> names = this.byName.get(region.getWorldName());
        if (names != null) {
            names.remove(region.getName().toLowerCase(Locale.ROOT), region);
        }
        if (region.isGlobal()) {
            this.globalByWorld.remove(region.getWorldName(), region);
        } else {
            ChunkRegionIndex index = this.indices.get(region.getWorldName());
            if (index != null) {
                index.remove(region);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> String serialize(Flag<T> flag, Object value) {
        return flag.serialize((T) value);
    }

    private SchedulerAdapter scheduler() {
        return this.plugin.getBootstrap().getScheduler();
    }
}
