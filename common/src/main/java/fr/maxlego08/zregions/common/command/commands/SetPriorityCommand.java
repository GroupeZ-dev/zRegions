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
 * Changes a region's priority (higher wins when regions overlap).
 */
public class SetPriorityCommand extends RegionCommand {

    public SetPriorityCommand() {
        super("setpriority", "zregions.admin", "setpriority <region> <priority>", "setpriority");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalRegion = args.getOpt(1);
        Optional<String> optionalPriority = args.getOpt(2);
        if (optionalRegion.isEmpty() || optionalPriority.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> region = resolveRegion(plugin, sender, optionalRegion.get());
        if (region.isEmpty()) {
            return;
        }

        int priority;
        try {
            priority = Integer.parseInt(optionalPriority.get());
        } catch (NumberFormatException exception) {
            sendUsage(plugin, sender);
            return;
        }

        plugin.getRegionManager().setPriority(region.get(), priority);
        plugin.getMessages().send(sender, Message.REGION_PRIORITY_SET,
                "region", region.get().getName(),
                "priority", String.valueOf(priority));
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .complete(args);
    }
}
