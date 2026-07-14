package fr.maxlego08.zregions.common.plugin.bootstrap;

import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import fr.maxlego08.zregions.common.plugin.scheduler.SchedulerAdapter;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * The platform bridge (LuckPerms' LuckPermsBootstrap model): holds the native objects,
 * exposes server primitives and player access. It contains ZERO business logic.
 */
public interface ZRegionsBootstrap {

    PluginLogger getPluginLogger();

    SchedulerAdapter getScheduler();

    /**
     * Latches counted down at the end of load()/enable(). Early async events
     * (a player joining during startup) await these before touching the plugin.
     */
    CountDownLatch getLoadLatch();

    CountDownLatch getEnableLatch();

    String getVersion();

    Instant getStartupTime();

    PlatformType getType();

    String getServerBrand();

    String getServerVersion();

    Path getDataDirectory();

    default InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    // --- online player access (the ONLY way common code reaches players) ---

    Optional<RegionPlayer> getPlayer(UUID uniqueId);

    Optional<UUID> lookupUniqueId(String username);

    Optional<String> lookupUsername(UUID uniqueId);

    Collection<UUID> getOnlinePlayers();

    Collection<String> getPlayerList();

    int getPlayerCount();

    boolean isPlayerOnline(UUID uniqueId);
}
