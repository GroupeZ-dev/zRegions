package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.selection.Selection;
import fr.maxlego08.zregions.common.selection.SelectionShapeBuilder;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Replaces a region's shape with the player's current selection — by default in
 * the region's CURRENT shape type, or in an explicitly given one (this is how a
 * cuboid becomes a cylinder). The selection must lie in the region's own world —
 * a region cannot move worlds.
 */
public class RedefineCommand extends RegionCommand {

    public RedefineCommand() {
        super("redefine", "zregions.admin", "redefine <name> [shape]", "redefine");
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
        Region region = optionalRegion.get();
        if (region.isGlobal()) {
            plugin.getMessages().send(sender, Message.REGION_REDEFINE_GLOBAL);
            return;
        }

        ShapeType type = region.getShape().getType();
        Optional<String> optionalShape = args.getOpt(2);
        if (optionalShape.isPresent()) {
            try {
                type = ShapeType.valueOf(optionalShape.get().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                sendUsage(plugin, sender);
                return;
            }
        }

        RegionPlayer player = optionalPlayer.get();
        Optional<Selection> optionalSelection = plugin.getSelectionManager().getSelection(player.getUniqueId());
        if (optionalSelection.isEmpty()) {
            plugin.getMessages().send(sender, Message.SELECTION_INCOMPLETE);
            return;
        }

        SelectionShapeBuilder.Result result = SelectionShapeBuilder.build(optionalSelection.get(), type);
        if (!result.isSuccess()) {
            sendSelectionError(plugin, sender, result.error());
            return;
        }
        if (!result.worldName().equals(region.getWorldName())) {
            plugin.getMessages().send(sender, Message.REGION_REDEFINE_WORLD_MISMATCH,
                    "world", result.worldName(),
                    "region_world", region.getWorldName());
            return;
        }

        region = plugin.getRegionManager().redefine(region, result.shape());
        plugin.getMessages().send(sender, Message.REGION_REDEFINED, "region", region.getName());
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, CompletionSupplier.startsWith(() -> Arrays.stream(ShapeType.values())
                        .map(type -> type.name().toLowerCase(Locale.ROOT))))
                .complete(args);
    }
}
