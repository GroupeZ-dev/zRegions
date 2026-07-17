package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.bukkit.listener.EnvironmentProtectionListener;
import fr.maxlego08.zregions.bukkit.listener.MovementListener;
import fr.maxlego08.zregions.bukkit.listener.PlayerStateListener;
import fr.maxlego08.zregions.bukkit.listener.ProtectionListener;
import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.AbstractZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
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
        this.playerFactory = new BukkitPlayerFactory(this.audiences);
    }

    @Override
    protected void registerPlatformListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        JavaPlugin loader = this.bootstrap.getLoader();
        pluginManager.registerEvents(new ProtectionListener(this), loader);
        pluginManager.registerEvents(new EnvironmentProtectionListener(this), loader);
        pluginManager.registerEvents(new PlayerStateListener(this), loader);
        pluginManager.registerEvents(new MovementListener(this), loader);
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

    @Override
    protected void setupPlatformHooks() {
        // Optional integrations (zMenu, WorldEdit, PlaceholderAPI…) plug in here later.
    }

    @Override
    protected void registerApiOnPlatform() {
        Bukkit.getServicesManager().register(RegionManager.class, getRegionManager(),
                this.bootstrap.getLoader(), ServicePriority.Highest);
    }

    @Override
    protected void performFinalSetup() {
        // Seed the movement tracker silently for players already online (server
        // /reload, plugin managers): no greeting replay, no ENTRY re-enforcement.
        for (Player player : Bukkit.getOnlinePlayers()) {
            RegionPlayer wrapped = this.playerFactory.wrap(player);
            getMovementTracker().seed(wrapped, wrapped.getLocation());
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
