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
 * Sets (or clears, when no parent is given) the parent a region inherits flags from.
 */
public class SetParentCommand extends RegionCommand {

    public SetParentCommand() {
        super("setparent", "zregions.admin", "setparent <region> [parent]", "setparent");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalRegion = args.getOpt(1);
        if (optionalRegion.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> region = resolveRegion(plugin, sender, optionalRegion.get());
        if (region.isEmpty()) {
            return;
        }

        Optional<String> optionalParent = args.getOpt(2);
        if (optionalParent.isEmpty()) {
            plugin.getRegionManager().setParent(region.get(), null);
            plugin.getMessages().send(sender, Message.REGION_PARENT_CLEARED, "region", region.get().getName());
            return;
        }

        Optional<Region> parent = resolveRegion(plugin, sender, optionalParent.get());
        if (parent.isEmpty()) {
            return;
        }

        try {
            plugin.getRegionManager().setParent(region.get(), parent.get());
        } catch (IllegalArgumentException exception) {
            plugin.getMessages().send(sender, Message.REGION_PARENT_CYCLE);
            return;
        }

        plugin.getMessages().send(sender, Message.REGION_PARENT_SET,
                "region", region.get().getName(),
                "parent", parent.get().getName());
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, regionNames(plugin))
                .complete(args);
    }
}
