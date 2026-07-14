package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.bukkit.listener.ProtectionListener;
import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.plugin.AbstractZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Bukkit implementation of the plugin: fills the {@link AbstractZRegionsPlugin}
 * hooks with platform wiring (factories, listeners, commands, services). All the
 * business logic stays in the common module.
 */
public final class ZRegionsBukkitPlugin extends AbstractZRegionsPlugin {

    private static final String[] DEFAULT_FILES = {
            "config.yml", "messages_en.yml", "messages_fr.yml", "messages_es.yml", "messages_it.yml"
    };
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
    protected void saveDefaultConfigs() {
        Path dataDirectory = this.bootstrap.getDataDirectory();
        try {
            Files.createDirectories(dataDirectory);
            for (String name : DEFAULT_FILES) {
                Path target = dataDirectory.resolve(name);
                if (Files.exists(target)) {
                    continue;
                }
                try (InputStream input = this.bootstrap.getResourceStream(name)) {
                    if (input == null) {
                        getLogger().warn("Default resource " + name + " is missing from the jar.");
                        continue;
                    }
                    Files.copy(input, target);
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to save the default configuration files", exception);
        }
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
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this.bootstrap.getLoader());
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
        // Nothing to do on Bukkit yet.
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
