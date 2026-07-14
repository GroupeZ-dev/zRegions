package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.sender.RegionSender;
import fr.maxlego08.zregions.common.sender.RegionSenderFactory;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Wraps Bukkit {@link CommandSender}s into {@link RegionSender}s. Messages go
 * through {@link BukkitAudiences} so they render identically on Spigot and Paper.
 */
public final class BukkitSenderFactory extends RegionSenderFactory<ZRegionsBukkitPlugin, CommandSender> {

    private final BukkitAudiences audiences;

    public BukkitSenderFactory(ZRegionsBukkitPlugin plugin, BukkitAudiences audiences) {
        super(plugin);
        this.audiences = audiences;
    }

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : RegionSender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSender sender) {
        return sender instanceof Player player ? player.getName() : RegionSender.CONSOLE_NAME;
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        this.audiences.sender(sender).sendMessage(message);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        Bukkit.dispatchCommand(sender, command);
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
    }

    @Override
    public void close() {
        this.audiences.close();
    }
}
