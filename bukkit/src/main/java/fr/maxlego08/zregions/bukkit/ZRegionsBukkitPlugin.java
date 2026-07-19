package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.bukkit.listener.EnvironmentProtectionListener;
import fr.maxlego08.zregions.bukkit.listener.MovementListener;
import fr.maxlego08.zregions.bukkit.listener.PlayerStateListener;
import fr.maxlego08.zregions.bukkit.listener.ProtectionListener;
import fr.maxlego08.zregions.bukkit.listener.WandListener;
import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.gui.GuiService;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.platform.RegionPlayerFactory;
import fr.maxlego08.zregions.common.plugin.AbstractZRegionsPlugin;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * The Bukkit implementation of the plugin: fills the {@link AbstractZRegionsPlugin}
 * hooks with platform wiring (factories, listeners, commands, services). All the
 * business logic stays in the common module.
 */
public final class ZRegionsBukkitPlugin extends AbstractZRegionsPlugin {

    private static final String[] COMMANDS = {"region", "zregions"};
    
    private static final int BSTATS_PLUGIN_ID = 32753;

    private final ZRegionsBukkitBootstrap bootstrap;
    private BukkitSenderFactory senderFactory;
    private BukkitPlayerFactory playerFactory;
    private BukkitAudiences audiences;
    private BukkitCommandExecutor commandExecutor;

    public ZRegionsBukkitPlugin(ZRegionsBukkitBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter(Path file) {
        return new BukkitConfigAdapter(file);
    }

    @Override
    protected void setupSenderFactory() {
        this.audiences = BukkitAudiences.create(this.bootstrap.getLoader());
        this.senderFactory = new BukkitSenderFactory(this, this.audiences);
    }

    @Override
    protected void setupPlayerFactory() {
        this.playerFactory = new BukkitPlayerFactory(this, this.audiences);
    }

    @Override
    protected void registerPlatformListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        JavaPlugin loader = this.bootstrap.getLoader();
        pluginManager.registerEvents(new ProtectionListener(this), loader);
        pluginManager.registerEvents(new EnvironmentProtectionListener(this), loader);
        pluginManager.registerEvents(new PlayerStateListener(this), loader);
        pluginManager.registerEvents(new MovementListener(this), loader);
        pluginManager.registerEvents(new WandListener(this), loader);
    }

    @Override
    protected void registerCommands() {
        this.commandExecutor = new BukkitCommandExecutor(this);
        for (String name : COMMANDS) {
            PluginCommand command = this.bootstrap.getLoader().getCommand(name);
            if (command == null) {
                getLogger().warn("Command /" + name + " is not defined in plugin.yml, skipping.");
                continue;
            }
            command.setExecutor(this.commandExecutor);
            command.setTabCompleter(this.commandExecutor);
        }
    }

    /**
     * Optional integrations. Hook classes are referenced only by name and
     * instantiated after the target plugin's presence check — the JVM never
     * links them on a server without the plugin (same guard as the Folia
     * scheduler adapter in the bootstrap).
     */
    @Override
    protected void setupPlatformHooks() {
        Plugin zMenu = Bukkit.getPluginManager().getPlugin("zMenu");
        if (zMenu != null && zMenu.isEnabled()) {
            try {
                Class<?> clazz = Class.forName("fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService");
                setGuiService((GuiService) clazz
                        .getConstructor(ZRegionsPlugin.class, JavaPlugin.class, RegionPlayerFactory.class)
                        .newInstance(this, this.bootstrap.getLoader(), this.playerFactory));
                getLogger().info("Hooked into zMenu " + zMenu.getDescription().getVersion() + " — GUI enabled.");
            } catch (Throwable throwable) {
                getLogger().warn("Unable to hook into zMenu, the GUI stays disabled (commands still work).", throwable);
            }
        }

        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderApi != null && placeholderApi.isEnabled()) {
            try {
                Class<?> clazz = Class.forName("fr.maxlego08.zregions.hooks.placeholderapi.ZRegionsExpansion");
                Object expansion = clazz.getConstructor(ZRegionsPlugin.class).newInstance(this);
                clazz.getMethod("register").invoke(expansion);
                getLogger().info("Hooked into PlaceholderAPI — %zregions_...% placeholders enabled.");
            } catch (Throwable throwable) {
                getLogger().warn("Unable to hook into PlaceholderAPI, its placeholders stay unavailable.", throwable);
            }
        }
    }

    @Override
    protected void registerApiOnPlatform() {
        Bukkit.getServicesManager().register(RegionManager.class, getRegionManager(),
                this.bootstrap.getLoader(), ServicePriority.Highest);
    }

    @Override
    protected void performFinalSetup() {
        setupMetrics();

        // Seed the movement tracker silently for players already online (server
        // /reload, plugin managers): no greeting replay, no ENTRY re-enforcement.
        for (Player player : Bukkit.getOnlinePlayers()) {
            RegionPlayer wrapped = this.playerFactory.wrap(player);
            getMovementTracker().seed(wrapped, wrapped.getLocation());
        }
    }

    /**
     * bStats telemetry (relocated). Disabled until {@link #BSTATS_PLUGIN_ID} holds
     * the real service id, so nothing is ever reported to another plugin's page.
     */
    private void setupMetrics() {
        if (BSTATS_PLUGIN_ID <= 0) {
            return;
        }
        try {
            Metrics metrics = new Metrics(this.bootstrap.getLoader(), BSTATS_PLUGIN_ID);
            metrics.addCustomChart(new SimplePie("storage_type", () -> getConfiguration().getStorageType()));
            metrics.addCustomChart(new SimplePie("language", this::getLanguage));
            metrics.addCustomChart(new SimplePie("multi_server",
                    () -> getConfiguration().isMultiServerEnabled() ? "enabled" : "disabled"));
            metrics.addCustomChart(new SingleLineChart("regions", () -> getRegionManager().getRegions().size()));
        } catch (Throwable throwable) {
            getLogger().warn("Unable to initialise bStats metrics.", throwable);
        }
    }

    @Override
    public ZRegionsBukkitBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    public RegionSender getConsoleSender() {
        return this.senderFactory.wrap(Bukkit.getConsoleSender());
    }

    public BukkitSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

    public BukkitPlayerFactory getPlayerFactory() {
        return this.playerFactory;
    }
}
