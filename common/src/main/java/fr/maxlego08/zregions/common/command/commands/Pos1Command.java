package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Optional;

/**
 * Sets the first selection corner to the player's current position.
 */
public class Pos1Command extends RegionCommand {

    public Pos1Command() {
        super("pos1", "zregions.admin", "pos1", "pos1");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }

        RegionPlayer player = optionalPlayer.get();
        RegionLocation location = player.getLocation();
        plugin.getSelectionManager().setPos1(player.getUniqueId(), location);
        plugin.getMessages().send(sender, Message.POS1_SET, "position", location.toString());
    }
}
