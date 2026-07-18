package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Optional;

/**
 * Empties the polygon vertex list of the player's selection, keeping both
 * positions.
 */
public class ClearPointsCommand extends RegionCommand {

    public ClearPointsCommand() {
        super("clearpoints", "zregions.admin", "clearpoints", "clearpoints");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }

        plugin.getSelectionManager().clearPoints(optionalPlayer.get().getUniqueId());
        plugin.getMessages().send(sender, Message.POINTS_CLEARED);
    }
}
