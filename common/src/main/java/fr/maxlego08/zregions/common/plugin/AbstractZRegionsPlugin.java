package fr.maxlego08.zregions.common.plugin;

import fr.maxlego08.zregions.api.flag.FlagRegistry;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.common.command.RegionCommandManager;
import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.flag.ZFlagRegistry;
import fr.maxlego08.zregions.common.gui.GuiService;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.movement.RegionMovementTracker;
import fr.maxlego08.zregions.common.region.ZRegionManager;
import fr.maxlego08.zregions.common.selection.SelectionManager;
import fr.maxlego08.zregions.common.storage.RegionStorage;
import fr.maxlego08.zregions.common.storage.SarahRegionStorage;
import fr.maxlego08.zregions.common.visual.BorderDisplayManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * The plugin lifecycle, defined ONCE for every platform (LuckPerms'
 * AbstractLuckPermsPlugin): {@link #load()}/{@link #enable()}/{@link #disable()}
 * are final and fix the startup order; platforms only fill the named hooks.
 */
public abstract class AbstractZRegionsPlugin implements ZRegionsPlugin {

    /** The languages whose default files ship in the jar. */
    private static final Set<String> BUNDLED_LANGUAGES = Set.of("en", "fr", "es", "it");

    private String language = "en";
    private ZRegionsConfiguration configuration;
    private MessageService messages;
    private RegionStorage storage;
    private ZFlagRegistry flagRegistry;
    private SelectionManager selectionManager;
    private ZRegionManager regionManager;
    private RegionMovementTracker movementTracker;
    private BorderDisplayManager borderDisplay;
    private RegionCommandManager commandManager;
    private GuiService guiService = GuiService.NONE;
    private boolean running = false;

    public final void load() {
        // language.yml first (never translated, root of the data folder): it picks
        // the language of every DEFAULT file extracted afterwards (zAuctionHouse model)
        this.language = resolveConfiguredLanguage();
        ensureDefaultFile("config.yml", "languages/" + this.language + "/config.yml");
        this.configuration = new ZRegionsConfiguration(
                provideConfigurationAdapter(getBootstrap().getDataDirectory().resolve("config.yml")));
    }

    /**
     * Extracts language.yml on first boot, then reads its {@code language} key:
     * {@code auto} (the default) resolves against the server's JVM locale, an
     * unknown code warns and falls back to English. The resolved value is always
     * one of the bundled languages.
     */
    private String resolveConfiguredLanguage() {
        Path file = getBootstrap().getDataDirectory().resolve("language.yml");
        if (!Files.exists(file)) {
            try (InputStream input = getBootstrap().getResourceStream("language.yml")) {
                if (input != null) {
                    Files.createDirectories(file.getParent());
                    Files.copy(input, file);
                }
            } catch (IOException exception) {
                getLogger().warn("Unable to extract language.yml, using automatic detection.", exception);
            }
        }
        String configured = Files.exists(file)
                ? provideConfigurationAdapter(file).getString("language", "auto")
                : "auto";

        String normalized = configured == null ? "auto" : configured.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("auto")) {
            normalized = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
            return BUNDLED_LANGUAGES.contains(normalized) ? normalized : "en";
        }
        if (!BUNDLED_LANGUAGES.contains(normalized)) {
            getLogger().warn("Unknown language '" + configured + "' in language.yml (available: "
                    + String.join(", ", BUNDLED_LANGUAGES) + " or auto), falling back to English.");
            return "en";
        }
        return normalized;
    }

    /**
     * Extracts a default file (in the configured language) on first boot. An
     * existing file is never touched — regenerating a default means deleting the
     * file and reloading/restarting, exactly like zAuctionHouse.
     */
    private void ensureDefaultFile(String diskName, String jarPath) {
        Path file = getBootstrap().getDataDirectory().resolve(diskName);
        if (Files.exists(file)) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            InputStream input = getBootstrap().getResourceStream(jarPath);
            if (input == null) {
                getLogger().warn("No bundled " + jarPath + " found in the jar, starting with built-in defaults.");
                return;
            }
            try (InputStream in = input) {
                Files.copy(in, file);
            }
        } catch (IOException exception) {
            getLogger().warn("Unable to extract the default " + diskName, exception);
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
        this.borderDisplay = new BorderDisplayManager(this);

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
        // re-read language.yml too: deleted default files regenerate in the new language
        this.language = resolveConfiguredLanguage();
        ensureDefaultFile("config.yml", "languages/" + this.language + "/config.yml");
        this.configuration.reload();
        this.messages.load(provideConfigurationAdapter(resolveMessagesFile()));
        this.guiService.reload();
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
     * The messages file, at the ROOT of the data folder: {@code messages.yml}.
     * Extracted on first use from the bundled translation of the configured
     * language; customizing means editing that one file (any language), and a
     * missing key always falls back to the English defaults of the Message enum.
     */
    private Path resolveMessagesFile() {
        ensureDefaultFile("messages.yml", "languages/" + this.language + "/messages.yml");
        return getBootstrap().getDataDirectory().resolve("messages.yml");
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

    @Override
    public BorderDisplayManager getBorderDisplay() {
        return this.borderDisplay;
    }

    @Override
    public GuiService getGuiService() {
        return this.guiService;
    }

    @Override
    public String getLanguage() {
        return this.language;
    }

    /** Installed by an optional platform hook during {@link #setupPlatformHooks()}. */
    protected void setGuiService(GuiService guiService) {
        this.guiService = guiService == null ? GuiService.NONE : guiService;
    }
}
