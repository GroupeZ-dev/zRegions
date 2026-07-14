package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.sender.RegionSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Binds Bukkit's command API to the common {@code RegionCommandManager}: converts
 * {@code CommandSender/String[]} to {@code RegionSender/List<String>} and delegates.
 */
public final class BukkitCommandExecutor implements TabExecutor {

    private final ZRegionsBukkitPlugin plugin;

    public BukkitCommandExecutor(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        RegionSender wrapped = this.plugin.getSenderFactory().wrap(sender);
        this.plugin.getCommandManager().executeCommand(wrapped, label, new ArrayList<>(Arrays.asList(args)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        RegionSender wrapped = this.plugin.getSenderFactory().wrap(sender);
        return this.plugin.getCommandManager().tabCompleteCommand(wrapped, new ArrayList<>(Arrays.asList(args)));
    }
}
