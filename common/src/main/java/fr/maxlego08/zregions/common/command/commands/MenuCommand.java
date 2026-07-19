package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.gui.GuiService;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;

/**
 * Opens the region GUI: the region list without argument, one region's menu with
 * one. The GUI is an optional layer ({@code Hooks/zMenu}) — without it the
 * command points back to the full text interface instead of failing.
 */
public class MenuCommand extends RegionCommand {

    public MenuCommand() {
        super("menu", "zregions.admin", "menu [region]", "menu");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }

        GuiService gui = plugin.getGuiService();
        if (!gui.isAvailable()) {
            plugin.getMessages().send(sender, Message.GUI_UNAVAILABLE);
            return;
        }

        RegionPlayer player = optionalPlayer.get();
        Optional<String> optionalRegion = args.getOpt(1);
        if (optionalRegion.isEmpty()) {
            gui.openRegionList(player);
            return;
        }

        Optional<Region> region = resolveRegion(plugin, sender, optionalRegion.get());
        if (region.isEmpty()) {
            return;
        }
        gui.openRegionMenu(player, region.get());
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .complete(args);
    }
}
