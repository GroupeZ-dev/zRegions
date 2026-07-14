package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

/**
 * Lists the sub-commands the sender is allowed to use.
 */
public class HelpCommand extends RegionCommand {

    public HelpCommand() {
        super("help", "zregions.use", "help", "help");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        plugin.getMessages().send(sender, Message.HELP_HEADER);
        for (RegionCommand command : plugin.getCommandManager().getCommands()) {
            if (!command.isAuthorized(sender)) {
                continue;
            }
            plugin.getMessages().send(sender, Message.HELP_ENTRY,
                    "usage", command.getUsage(),
                    "description", command.getUsage());
        }
    }
}
