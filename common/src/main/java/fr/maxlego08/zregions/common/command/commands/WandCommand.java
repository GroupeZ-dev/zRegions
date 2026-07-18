package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Optional;

/**
 * Hands the player the selection wand (left click = pos1, right click = pos2).
 * The item is provided by the platform, marked so renaming does not break it.
 */
public class WandCommand extends RegionCommand {

    public WandCommand() {
        super("wand", "zregions.admin", "wand", "wand");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }

        optionalPlayer.get().giveWand();
        plugin.getMessages().send(sender, Message.WAND_GIVEN);
    }
}
