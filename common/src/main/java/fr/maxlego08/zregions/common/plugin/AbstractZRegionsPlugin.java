package fr.maxlego08.zregions.common.plugin;

import fr.maxlego08.zregions.api.flag.FlagRegistry;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.common.command.RegionCommandManager;
import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.flag.ZFlagRegistry;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.region.ZRegionManager;
import fr.maxlego08.zregions.common.selection.SelectionManager;
import fr.maxlego08.zregions.common.storage.RegionStorage;
import fr.maxlego08.zregions.common.storage.SarahRegionStorage;

import java.nio.file.Path;

/**
 * The plugin lifecycle, defined ONCE for every platform (LuckPerms'
 * AbstractLuckPermsPlugin): {@link #load()}/{@link #enable()}/{@link #disable()}
 * are final and fix the startup order; platforms only fill the named hooks.
 */
public abstract class AbstractZRegionsPlugin implements ZRegionsPlugin {

    private ZRegionsConfiguration configuration;
    private MessageService messages;
    private RegionStorage storage;
    private ZFlagRegistry flagRegistry;
    private SelectionManager selectionManager;
    private ZRegionManager regionManager;
    private RegionCommandManager commandManager;
    private boolean running = false;

    public final void load() {
        saveDefaultConfigs();
        this.configuration = new ZRegionsConfiguration(
                provideConfigurationAdapter(getBootstrap().getDataDirectory().resolve("config.yml")));
    }

    public final void enable() {
        // 1. platform bridges
        setupSenderFactory();
        setupPlayerFactory();

        // 2. messages
        this.messages = new MessageService();
        this.messages.load(provideConfigurationAdapter(resolveMessagesFile()));

        // 3. flags (before regions: stored values need the registry to parse)
        this.flagRegistry = new ZFlagRegistry();
        Flags.registerAll(this.flagRegistry);

        // 4. storage, then the in-memory cache + spatial index
        this.storage = new SarahRegionStorage(this);
        try {
            this.storage.connect();
        } catch (Exception exception) {
            getLogger().severe("Unable to connect to the region storage, disabling.", exception);
            throw new IllegalStateException("Storage connection failed", exception);
        }
        this.selectionManager = new SelectionManager();
        this.regionManager = new ZRegionManager(this);
        this.regionManager.loadAllBlocking();

        // 5. commands & platform wiring
        this.commandManager = new RegionCommandManager(this);
        registerPlatformListeners();
        registerCommands();
        setupPlatformHooks();
        registerApiOnPlatform();
        performFinalSetup();

        this.running = true;
        getLogger().info("zRegions " + getBootstrap().getVersion() + " enabled ("
                + this.regionManager.getRegions().size() + " region(s) loaded, server '"
                + this.configuration.getServerName() + "').");
    }

    public final void disable() {
        this.running = false;
        getBootstrap().getScheduler().shutdownScheduler();
        if (this.storage != null) {
            this.storage.disconnect();
        }
        getBootstrap().getScheduler().shutdownExecutor();
    }

    private Path resolveMessagesFile() {
        String language = this.configuration.getLanguage();
        Path file = getBootstrap().getDataDirectory().resolve("messages_" + language + ".yml");
        if (!java.nio.file.Files.exists(file)) {
            getLogger().warn("Language file messages_" + language + ".yml not found, falling back to English.");
            file = getBootstrap().getDataDirectory().resolve("messages_en.yml");
        }
        return file;
    }

    public boolean isRunning() {
        return this.running;
    }

    // --- the platform contract: hooks, in enable() order ---

    /** Copies default config.yml + messages_*.yml to the data directory when absent. */
    protected abstract void saveDefaultConfigs();

    /** Creates a platform view over a YAML file (Bukkit: YamlConfiguration). */
    protected abstract ConfigurationAdapter provideConfigurationAdapter(Path file);

    protected abstract void setupSenderFactory();

    protected abstract void setupPlayerFactory();

    protected abstract void registerPlatformListeners();

    protected abstract void registerCommands();

    protected abstract void setupPlatformHooks();

    protected abstract void registerApiOnPlatform();

    protected abstract void performFinalSetup();

    // --- getters ---

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
        return this.commandManager;
    }

    @Override
    public SelectionManager getSelectionManager() {
        return this.selectionManager;
    }
}
