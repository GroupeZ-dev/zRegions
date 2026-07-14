package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.selection.Selection;
import fr.maxlego08.zregions.common.sender.RegionSender;
import fr.maxlego08.zregions.common.shape.CuboidShape;

import java.util.Optional;

/**
 * Creates a region from the player's current selection.
 */
public class CreateCommand extends RegionCommand {

    public CreateCommand() {
        super("create", "zregions.admin", "create <name> [priority]", "create");
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

        String name = optionalName.get();
        int priority = args.getIntOrDefault(2, 0);
        try {
            Region region = plugin.getRegionManager().createRegion(
                    selection.getWorldName(), name, optionalShape.get(), priority, player.getUniqueId());
            plugin.getMessages().send(sender, Message.REGION_CREATED,
                    "region", region.getName(),
                    "shape", region.getShape().getType().name(),
                    "priority", String.valueOf(region.getPriority()));
        } catch (IllegalArgumentException exception) {
            plugin.getMessages().send(sender, Message.REGION_ALREADY_EXISTS, "region", name);
        }
    }
}
