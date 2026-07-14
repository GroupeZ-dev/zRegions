package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Lists the regions of a world: the given one, the player's world, or — for the
 * console without argument — every world holding at least one region.
 */
public class ListCommand extends RegionCommand {

    public ListCommand() {
        super("list", "zregions.use", "list [world]", "list");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalWorld = args.getOpt(1);
        if (optionalWorld.isPresent()) {
            String worldName = optionalWorld.get();
            sendWorld(plugin, sender, worldName, plugin.getRegionManager().getRegions(worldName));
            return;
        }

        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isPresent()) {
            String worldName = optionalPlayer.get().getWorldName();
            sendWorld(plugin, sender, worldName, plugin.getRegionManager().getRegions(worldName));
            return;
        }

        Map<String, List<Region>> byWorld = plugin.getRegionManager().getRegions().stream()
                .collect(Collectors.groupingBy(Region::getWorldName, TreeMap::new, Collectors.toList()));
        if (byWorld.isEmpty()) {
            plugin.getMessages().send(sender, Message.REGION_LIST_EMPTY);
            return;
        }
        byWorld.forEach((worldName, regions) -> sendWorld(plugin, sender, worldName, regions));
    }

    private void sendWorld(ZRegionsPlugin plugin, RegionSender sender, String worldName, Collection<Region> regions) {
        if (regions.isEmpty()) {
            plugin.getMessages().send(sender, Message.REGION_LIST_EMPTY);
            return;
        }
        plugin.getMessages().send(sender, Message.REGION_LIST_HEADER,
                "world", worldName,
                "count", String.valueOf(regions.size()));
        for (Region region : regions) {
            plugin.getMessages().send(sender, Message.REGION_LIST_ENTRY,
                    "region", region.getName(),
                    "shape", region.getShape().getType().name(),
                    "priority", String.valueOf(region.getPriority()));
        }
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, CompletionSupplier.startsWith(() -> plugin.getRegionManager().getRegions().stream()
                        .map(Region::getWorldName)
                        .distinct()))
                .complete(args);
    }
}
