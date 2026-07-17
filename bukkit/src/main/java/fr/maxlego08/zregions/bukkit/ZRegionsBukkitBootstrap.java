package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.bootstrap.LoaderBootstrap;
import fr.maxlego08.zregions.common.plugin.bootstrap.PlatformType;
import fr.maxlego08.zregions.common.plugin.bootstrap.ZRegionsBootstrap;
import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * The Bukkit platform bridge: holds the native objects (loader, logger, scheduler),
 * exposes the server primitives to common code and drives the plugin lifecycle
 * through latches (LuckPerms' LPBukkitBootstrap model). Zero business logic.
 */
public final class ZRegionsBukkitBootstrap implements ZRegionsBootstrap, LoaderBootstrap {

    private static final String FOLIA_CLASS = "io.papermc.paper.threadedregions.RegionizedServer";
    private static final String FOLIA_SCHEDULER_CLASS = "fr.maxlego08.zregions.paper.FoliaSchedulerAdapter";

    private final JavaPlugin loader;
    private final PluginLogger logger;
    private final SchedulerAdapter scheduler;
    private final ZRegionsBukkitPlugin plugin;

    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);
    private Instant startupTime;

    public ZRegionsBukkitBootstrap(JavaPlugin loader) {
        this.loader = loader;
        this.logger = new JavaPluginLogger(loader.getLogger());
        this.scheduler = createScheduler(loader, this.logger);
        this.plugin = new ZRegionsBukkitPlugin(this);
    }

    /**
     * Folia is detected at runtime; the Folia adapter (paper sourceSet, imports
     * io.papermc.*) is only ever loaded by reflection so pure Spigot never links it.
     */
    private static SchedulerAdapter createScheduler(JavaPlugin loader, PluginLogger logger) {
        if (classExists(FOLIA_CLASS)) {
            try {
                Class<?> clazz = Class.forName(FOLIA_SCHEDULER_CLASS);
                return (SchedulerAdapter) clazz.getConstructor(Plugin.class, PluginLogger.class)
                        .newInstance(loader, logger);
            } catch (ReflectiveOperationException exception) {
                logger.warn("Unable to load the Folia scheduler adapter, falling back to the Bukkit scheduler.", exception);
            }
        }
        return new BukkitSchedulerAdapter(loader, logger);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    // --- lifecycle ---

    @Override
    public void onLoad() {
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }
    }

    @Override
    public void onEnable() {
        this.startupTime = Instant.now();
        try {
            this.plugin.enable();
        } catch (Exception exception) {
            this.logger.severe("An error occurred while enabling zRegions, disabling the plugin.", exception);
            Bukkit.getPluginManager().disablePlugin(this.loader);
        } finally {
            this.enableLatch.countDown();
        }
    }

    @Override
    public void onDisable() {
        this.plugin.disable();
    }

    // --- platform primitives ---

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
        return this.loadLatch;
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public String getVersion() {
        return this.loader.getDescription().getVersion();
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
        return Bukkit.getName();
    }

    @Override
    public String getServerVersion() {
        return Bukkit.getVersion();
    }

    @Override
    public Path getDataDirectory() {
        return this.loader.getDataFolder().toPath().toAbsolutePath();
    }

    // --- online player access ---

    @Override
    public Optional<RegionPlayer> getPlayer(UUID uniqueId) {
        Player player = Bukkit.getPlayer(uniqueId);
        if (player == null || this.plugin.getPlayerFactory() == null) {
            return Optional.empty();
        }
        return Optional.of(this.plugin.getPlayerFactory().wrap(player));
    }

    /**
     * Bukkit's getOfflinePlayer(name) fabricates a profile for ANY name — an
     * unknown player must resolve to empty, or member commands would silently
     * persist garbage UUIDs on typos.
     */
    @Override
    @SuppressWarnings("deprecation")
    public Optional<UUID> lookupUniqueId(String username) {
        Player online = Bukkit.getPlayerExact(username);
        if (online != null) {
            return Optional.of(online.getUniqueId());
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(username);
        return offline.hasPlayedBefore() || offline.isOnline()
                ? Optional.of(offline.getUniqueId())
                : Optional.empty();
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        return Optional.ofNullable(Bukkit.getOfflinePlayer(uniqueId).getName());
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        List<UUID> uuids = new ArrayList<>(players.size());
        for (Player player : players) {
            uuids.add(player.getUniqueId());
        }
        return uuids;
    }

    @Override
    public Collection<String> getPlayerList() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        List<String> names = new ArrayList<>(players.size());
        for (Player player : players) {
            names.add(player.getName());
        }
        return names;
    }

    @Override
    public int getPlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        Player player = Bukkit.getPlayer(uniqueId);
        return player != null && player.isOnline();
    }

    // --- bukkit accessors ---

    public JavaPlugin getLoader() {
        return this.loader;
    }

    public ZRegionsBukkitPlugin getPluginInstance() {
        return this.plugin;
    }
}
