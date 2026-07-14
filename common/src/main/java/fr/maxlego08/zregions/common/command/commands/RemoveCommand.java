package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;

/**
 * Deletes a region, looked up by name across all worlds.
 */
public class RemoveCommand extends RegionCommand {

    public RemoveCommand() {
        super("remove", "zregions.admin", "remove <name>", "remove");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalName = args.getOpt(1);
        if (optionalName.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        String name = optionalName.get();
        Optional<Region> optionalRegion = findRegion(plugin, name);
        if (optionalRegion.isEmpty()) {
            plugin.getMessages().send(sender, Message.REGION_NOT_FOUND, "region", name);
            return;
        }

        Region region = optionalRegion.get();
        plugin.getRegionManager().deleteRegion(region);
        plugin.getMessages().send(sender, Message.REGION_REMOVED, "region", region.getName());
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .complete(args);
    }
}
