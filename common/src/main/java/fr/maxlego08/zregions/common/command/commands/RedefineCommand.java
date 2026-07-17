package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.selection.Selection;
import fr.maxlego08.zregions.common.sender.RegionSender;
import fr.maxlego08.zregions.common.shape.CuboidShape;

import java.util.List;
import java.util.Optional;

/**
 * Replaces a region's shape with the player's current selection. The selection
 * must lie in the region's own world — a region cannot move worlds.
 */
public class RedefineCommand extends RegionCommand {

    public RedefineCommand() {
        super("redefine", "zregions.admin", "redefine <name>", "redefine");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }
        Optional<String> optionalName = args.getOpt(1);
        if (optionalName.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> optionalRegion = resolveRegion(plugin, sender, optionalName.get());
        if (optionalRegion.isEmpty()) {
            return;
        }

        RegionPlayer player = optionalPlayer.get();
        Optional<Selection> optionalSelection = plugin.getSelectionManager().getSelection(player.getUniqueId());
        if (optionalSelection.isEmpty() || !optionalSelection.get().isComplete()) {
            plugin.getMessages().send(sender, Message.SELECTION_INCOMPLETE);
            return;
        }

        Selection selection = optionalSelection.get();
        if (!selection.isSameWorld()) {
            plugin.getMessages().send(sender, Message.SELECTION_WORLD_MISMATCH);
            return;
        }
        Optional<CuboidShape> optionalShape = selection.toCuboid();
        if (optionalShape.isEmpty()) {
            plugin.getMessages().send(sender, Message.SELECTION_INCOMPLETE);
            return;
        }

        Region region = optionalRegion.get();
        if (region.isGlobal()) {
            plugin.getMessages().send(sender, Message.REGION_REDEFINE_GLOBAL);
            return;
        }
        if (!selection.getWorldName().equals(region.getWorldName())) {
            plugin.getMessages().send(sender, Message.REGION_REDEFINE_WORLD_MISMATCH,
                    "world", selection.getWorldName(),
                    "region_world", region.getWorldName());
            return;
        }

        region = plugin.getRegionManager().redefine(region, optionalShape.get());
        plugin.getMessages().send(sender, Message.REGION_REDEFINED, "region", region.getName());
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .complete(args);
    }
}
