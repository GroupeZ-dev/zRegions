package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.manager.RegionManager;
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
 * Creates a region from the player's current selection, in any of the four
 * shapes (default: cuboid). The optional shape argument sits before the
 * optional priority: {@code /rg create <name> [shape] [priority]} —
 * unambiguous because shape names are never integers.
 */
public class CreateCommand extends RegionCommand {

    public CreateCommand() {
        super("create", "zregions.admin", "create <name> [shape] [priority]", "create");
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

        // optional shape at index 2 shifts the priority to index 3
        ShapeType type = ShapeType.CUBOID;
        int priorityIndex = 2;
        Optional<ShapeType> optionalType = args.getOpt(2).flatMap(CreateCommand::parseShape);
        if (optionalType.isPresent()) {
            type = optionalType.get();
            priorityIndex = 3;
        }
        int priority = args.getIntOrDefault(priorityIndex, 0);

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

        String name = optionalName.get();
        if (RegionManager.GLOBAL_REGION_NAME.equalsIgnoreCase(name)) {
            plugin.getMessages().send(sender, Message.REGION_NAME_RESERVED, "region", name);
            return;
        }
        try {
            Region region = plugin.getRegionManager().createRegion(
                    result.worldName(), name, result.shape(), priority, player.getUniqueId());
            plugin.getMessages().send(sender, Message.REGION_CREATED,
                    "region", region.getName(),
                    "shape", region.getShape().getType().name(),
                    "priority", String.valueOf(region.getPriority()));
        } catch (IllegalArgumentException exception) {
            plugin.getMessages().send(sender, Message.REGION_ALREADY_EXISTS, "region", name);
        }
    }

    private static Optional<ShapeType> parseShape(String input) {
        try {
            return Optional.of(ShapeType.valueOf(input.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(2, CompletionSupplier.startsWith(() -> Arrays.stream(ShapeType.values())
                        .map(type -> type.name().toLowerCase(Locale.ROOT))))
                .complete(args);
    }
}
