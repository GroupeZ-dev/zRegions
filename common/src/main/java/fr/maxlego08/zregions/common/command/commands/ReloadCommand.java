package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

/**
 * Reloads the configuration file and the messages of the (possibly changed) language.
 */
public class ReloadCommand extends RegionCommand {

    public ReloadCommand() {
        super("reload", "zregions.admin", "reload", "reload");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        plugin.reload();
        plugin.getMessages().send(sender, Message.RELOADED);
    }
}
