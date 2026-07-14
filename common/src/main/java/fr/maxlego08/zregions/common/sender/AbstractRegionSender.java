package fr.maxlego08.zregions.common.sender;

import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * The single generic {@link RegionSender} implementation — identity is captured at
 * wrap time, behavior is delegated to the platform factory (LuckPerms' AbstractSender).
 */
final class AbstractRegionSender<T> implements RegionSender {

    private final ZRegionsPlugin plugin;
    private final RegionSenderFactory<?, T> factory;
    private final T sender;

    private final UUID uniqueId;
    private final String name;
    private final boolean console;

    AbstractRegionSender(ZRegionsPlugin plugin, RegionSenderFactory<?, T> factory, T sender) {
        this.plugin = plugin;
        this.factory = factory;
        this.sender = sender;
        this.uniqueId = factory.getUniqueId(sender);
        this.name = factory.getName(sender);
        this.console = factory.isConsole(sender);
    }

    @Override
    public ZRegionsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public void sendMessage(Component message) {
        this.factory.sendMessage(this.sender, message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return isConsole() || this.factory.hasPermission(this.sender, permission);
    }

    @Override
    public void performCommand(String commandLine) {
        this.factory.performCommand(this.sender, commandLine);
    }

    @Override
    public boolean isConsole() {
        return this.console;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractRegionSender<?> other)) return false;
        return this.uniqueId.equals(other.uniqueId);
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }
}
