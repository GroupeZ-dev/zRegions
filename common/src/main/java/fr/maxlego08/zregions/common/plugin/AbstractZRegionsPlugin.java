package fr.maxlego08.zregions.common.plugin;

import fr.maxlego08.zregions.api.flag.FlagRegistry;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.common.command.RegionCommandManager;
import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.flag.ZFlagRegistry;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.movement.RegionMovementTracker;
import fr.maxlego08.zregions.common.region.ZRegionManager;
import fr.maxlego08.zregions.common.selection.SelectionManager;
import fr.maxlego08.zregions.common.storage.RegionStorage;
import fr.maxlego08.zregions.common.storage.SarahRegionStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

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
    private RegionMovementTracker movementTracker;
    private RegionCommandManager commandManager;
    private boolean running = false;

    public final void load() {
        ensureDefaultConfig();
        this.configuration = new ZRegionsConfiguration(
                provideConfigurationAdapter(getBootstrap().getDataDirectory().resolve("config.yml")));
    }

    /**
     * Extracts the default config.yml on first boot, picking the translation
     * matching the server's JVM locale ({@code languages/<locale>/config.yml},
     * falling back to English). The translated file also presets {@code language:}
     * to its own language, so a French-locale server is French out of the box.
     * An existing config.yml is never touched.
     */
    private void ensureDefaultConfig() {
        Path file = getBootstrap().getDataDirectory().resolve("config.yml");
        if (Files.exists(file)) {
            return;
        }
        String locale = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
        try {
            Files.createDirectories(file.getParent());
            InputStream input = getBootstrap().getResourceStream("languages/" + locale + "/config.yml");
            if (input == null) {
                input = getBootstrap().getResourceStream("languages/en/config.yml");
            }
            if (input == null) {
                getLogger().warn("No bundled config.yml found in the jar, starting with built-in defaults.");
                return;
            }
            try (InputStream in = input) {
                Files.copy(in, file);
            }
        } catch (IOException exception) {
            getLogger().warn("Unable to extract the default config.yml", exception);
        }
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
        this.movementTracker = new RegionMovementTracker(this);

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

    @Override
    public final void reload() {
        this.configuration.reload();
        this.messages.load(provideConfigurationAdapter(resolveMessagesFile()));
    }

    public final void disable() {
        this.running = false;
        getBootstrap().getScheduler().shutdownScheduler();
        if (this.storage != null) {
            this.storage.disconnect();
        }
        getBootstrap().getScheduler().shutdownExecutor();
    }

    /**
     * The messages file of the configured language: {@code languages/<lang>/messages.yml}.
     * Only that one folder is ever extracted from the jar — the other bundled
     * languages stay inside it. Unknown language (no folder on disk, nothing
     * bundled) falls back to English.
     */
    private Path resolveMessagesFile() {
        String language = this.configuration.getLanguage();
        Path file = ensureLanguageFile(language);
        if (file == null) {
            getLogger().warn("No messages for language '" + language
                    + "' (neither in languages/" + language + "/ nor bundled), falling back to English.");
            file = ensureLanguageFile("en");
        }
        return file != null ? file
                : getBootstrap().getDataDirectory().resolve("languages").resolve("en").resolve("messages.yml");
    }

    /**
     * The on-disk messages file for {@code language}, extracting the bundled
     * default on first use. Returns {@code null} when the language exists neither
     * on disk nor in the jar (server owners may create languages/xx/messages.yml
     * by hand for unbundled languages).
     */
    private Path ensureLanguageFile(String language) {
        Path file = getBootstrap().getDataDirectory()
                .resolve("languages").resolve(language).resolve("messages.yml");
        if (Files.exists(file)) {
            return file;
        }
        try (InputStream input = getBootstrap().getResourceStream("languages/" + language + "/messages.yml")) {
            if (input == null) {
                return null;
            }
            Files.createDirectories(file.getParent());
            Files.copy(input, file);
            return file;
        } catch (IOException exception) {
            getLogger().warn("Unable to extract languages/" + language + "/messages.yml", exception);
            return null;
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    // --- the platform contract: hooks, in enable() order ---

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

    @Override
    public RegionMovementTracker getMovementTracker() {
        return this.movementTracker;
    }
}
