package fr.maxlego08.zregions.common.sender;

import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Factory bridging the platform's native command source type to {@link RegionSender}
 * (1:1 port of LuckPerms' SenderFactory, MIT).
 *
 * @param <P> the plugin type
 * @param <T> the native command source type (org.bukkit.command.CommandSender on Bukkit)
 */
public abstract class RegionSenderFactory<P extends ZRegionsPlugin, T> implements AutoCloseable {

    private final P plugin;

    protected RegionSenderFactory(P plugin) {
        this.plugin = plugin;
    }

    protected P getPlugin() {
        return this.plugin;
    }

    protected abstract UUID getUniqueId(T sender);

    protected abstract String getName(T sender);

    protected abstract void sendMessage(T sender, Component message);

    protected abstract boolean hasPermission(T sender, String permission);

    protected abstract void performCommand(T sender, String command);

    protected abstract boolean isConsole(T sender);

    public final RegionSender wrap(T sender) {
        Objects.requireNonNull(sender, "sender");
        return new AbstractRegionSender<>(this.plugin, this, sender);
    }

    @Override
    public void close() {
    }
}
