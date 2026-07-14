package fr.maxlego08.zregions.common.region;

import fr.maxlego08.zregions.api.flag.FlagRegistry;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.common.command.RegionCommandManager;
import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.flag.ZFlagRegistry;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.plugin.bootstrap.PlatformType;
import fr.maxlego08.zregions.common.plugin.bootstrap.ZRegionsBootstrap;
import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import fr.maxlego08.zregions.common.plugin.scheduler.JavaSchedulerAdapter;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerAdapter;
import fr.maxlego08.zregions.common.selection.SelectionManager;
import fr.maxlego08.zregions.common.sender.RegionSender;
import fr.maxlego08.zregions.common.storage.RegionStorage;
import fr.maxlego08.zregions.common.storage.StoredRegion;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;

/**
 * In-memory {@link ZRegionsPlugin} stub for region/flag tests: a direct
 * (same-thread) scheduler so async persistence and reload run deterministically,
 * a HashMap-backed storage, a default configuration and a real flag registry.
 */
final class TestPluginFixture implements ZRegionsPlugin {

    private final TestLogger logger = new TestLogger();
    private final StubBootstrap bootstrap = new StubBootstrap(this.logger);
    private final InMemoryRegionStorage storage = new InMemoryRegionStorage();
    private final ZRegionsConfiguration configuration = new ZRegionsConfiguration(new StubConfigurationAdapter());
    private final MessageService messages = new MessageService();
    private final ZFlagRegistry flagRegistry = new ZFlagRegistry();
    private ZRegionManager regionManager;

    TestPluginFixture() {
        Flags.registerAll(this.flagRegistry);
    }

    void setRegionManager(ZRegionManager regionManager) {
        this.regionManager = regionManager;
    }

    InMemoryRegionStorage storage() {
        return this.storage;
    }

    @Override
    public ZRegionsBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    public ZRegionsConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public MessageService getMessages() {
        return this.messages;
    }

    @Override
    public RegionStorage getStorage() {
        return this.storage;
    }

    @Override
    public RegionManager getRegionManager() {
        return this.regionManager;
    }

    @Override
    public FlagRegistry getFlagRegistry() {
        return this.flagRegistry;
    }

    @Override
    public RegionCommandManager getCommandManager() {
        return null;
    }

    @Override
    public SelectionManager getSelectionManager() {
        return null;
    }

    @Override
    public RegionSender getConsoleSender() {
        return null;
    }

    /** Scheduler where async == sync == the calling thread. */
    static final class DirectSchedulerAdapter extends JavaSchedulerAdapter {

        DirectSchedulerAdapter(PluginLogger logger) {
            super(logger);
        }

        @Override
        public Executor async() {
            return Runnable::run;
        }

        @Override
        public void executeSync(Runnable task) {
            task.run();
        }
    }

    static final class TestLogger implements PluginLogger {

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void warn(String message, Throwable throwable) {
        }

        @Override
        public void severe(String message) {
        }

        @Override
        public void severe(String message, Throwable throwable) {
        }
    }

    /** Always answers the provided defaults. */
    static final class StubConfigurationAdapter implements ConfigurationAdapter {

        @Override
        public String getString(String path, String def) {
            return def;
        }

        @Override
        public int getInt(String path, int def) {
            return def;
        }

        @Override
        public boolean getBoolean(String path, boolean def) {
            return def;
        }

        @Override
        public List<String> getStringList(String path, List<String> def) {
            return def;
        }

        @Override
        public Collection<String> getKeys(String path) {
            return List.of();
        }

        @Override
        public void reload() {
        }
    }

    static final class StubBootstrap implements ZRegionsBootstrap {

        private final PluginLogger logger;
        private final SchedulerAdapter scheduler;
        private final Instant startupTime = Instant.now();

        StubBootstrap(PluginLogger logger) {
            this.logger = logger;
            this.scheduler = new DirectSchedulerAdapter(logger);
        }

        @Override
        public PluginLogger getPluginLogger() {
            return this.logger;
        }

        @Override
        public SchedulerAdapter getScheduler() {
            return this.scheduler;
        }

        @Override
        public CountDownLatch getLoadLatch() {
            return new CountDownLatch(0);
        }

        @Override
        public CountDownLatch getEnableLatch() {
            return new CountDownLatch(0);
        }

        @Override
        public String getVersion() {
            return "test";
        }

        @Override
        public Instant getStartupTime() {
            return this.startupTime;
        }

        @Override
        public PlatformType getType() {
            return PlatformType.BUKKIT;
        }

        @Override
        public String getServerBrand() {
            return "test";
        }

        @Override
        public String getServerVersion() {
            return "0";
        }

        @Override
        public Path getDataDirectory() {
            return Path.of("build", "test-data");
        }

        @Override
        public Optional<RegionPlayer> getPlayer(UUID uniqueId) {
            return Optional.empty();
        }

        @Override
        public Optional<UUID> lookupUniqueId(String username) {
            return Optional.empty();
        }

        @Override
        public Optional<String> lookupUsername(UUID uniqueId) {
            return Optional.empty();
        }

        @Override
        public Collection<UUID> getOnlinePlayers() {
            return List.of();
        }

        @Override
        public Collection<String> getPlayerList() {
            return List.of();
        }

        @Override
        public int getPlayerCount() {
            return 0;
        }

        @Override
        public boolean isPlayerOnline(UUID uniqueId) {
            return false;
        }
    }

    /**
     * HashMap-backed storage. Like the real schema, saveRegion never touches
     * flags/members: those go through saveFlag/saveMember, which rebuild the record.
     */
    static final class InMemoryRegionStorage implements RegionStorage {

        private final Map<UUID, StoredRegion> regions = new HashMap<>();

        /** Seeds a full record (flags/members included), bypassing saveRegion semantics. */
        void put(StoredRegion region) {
            this.regions.put(region.id(), region);
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public List<StoredRegion> loadRegions(String serverName) {
            return new ArrayList<>(this.regions.values());
        }

        @Override
        public Optional<StoredRegion> loadRegion(UUID id) {
            return Optional.ofNullable(this.regions.get(id));
        }

        @Override
        public void saveRegion(StoredRegion region) {
            StoredRegion existing = this.regions.get(region.id());
            List<StoredRegion.StoredFlag> flags = existing == null ? List.of() : existing.flags();
            List<StoredRegion.StoredMember> members = existing == null ? List.of() : existing.members();
            this.regions.put(region.id(), new StoredRegion(region.id(), region.name(), region.world(),
                    region.priority(), region.shapeType(), region.shapeData(), region.parentId(), region.global(),
                    region.originServer(), region.version(), flags, members));
        }

        @Override
        public void deleteRegion(UUID id) {
            this.regions.remove(id);
        }

        @Override
        public void saveFlag(UUID regionId, String flagKey, String groupTarget, String value) {
            mutate(regionId, region -> {
                List<StoredRegion.StoredFlag> flags = new ArrayList<>(region.flags());
                flags.removeIf(flag -> flag.flagKey().equals(flagKey) && flag.groupTarget().equals(groupTarget));
                flags.add(new StoredRegion.StoredFlag(flagKey, groupTarget, value));
                return withChildren(region, flags, region.members());
            });
        }

        @Override
        public void deleteFlag(UUID regionId, String flagKey, String groupTarget) {
            mutate(regionId, region -> {
                List<StoredRegion.StoredFlag> flags = new ArrayList<>(region.flags());
                flags.removeIf(flag -> flag.flagKey().equals(flagKey) && flag.groupTarget().equals(groupTarget));
                return withChildren(region, flags, region.members());
            });
        }

        @Override
        public void saveMember(UUID regionId, UUID playerId, String role) {
            mutate(regionId, region -> {
                List<StoredRegion.StoredMember> members = new ArrayList<>(region.members());
                members.removeIf(member -> member.playerId().equals(playerId));
                members.add(new StoredRegion.StoredMember(playerId, role));
                return withChildren(region, region.flags(), members);
            });
        }

        @Override
        public void deleteMember(UUID regionId, UUID playerId) {
            mutate(regionId, region -> {
                List<StoredRegion.StoredMember> members = new ArrayList<>(region.members());
                members.removeIf(member -> member.playerId().equals(playerId));
                return withChildren(region, region.flags(), members);
            });
        }

        private void mutate(UUID regionId, UnaryOperator<StoredRegion> operator) {
            StoredRegion region = this.regions.get(regionId);
            if (region != null) {
                this.regions.put(regionId, operator.apply(region));
            }
        }

        private static StoredRegion withChildren(StoredRegion region, List<StoredRegion.StoredFlag> flags,
                                                 List<StoredRegion.StoredMember> members) {
            return new StoredRegion(region.id(), region.name(), region.world(), region.priority(),
                    region.shapeType(), region.shapeData(), region.parentId(), region.global(),
                    region.originServer(), region.version(), flags, members);
        }
    }
}
