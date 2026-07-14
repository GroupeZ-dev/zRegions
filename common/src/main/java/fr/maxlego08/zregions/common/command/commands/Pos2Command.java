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
 * Sets the second selection corner to the player's current position.
 */
public class Pos2Command extends RegionCommand {

    public Pos2Command() {
        super("pos2", "zregions.admin", "pos2", "pos2");
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
        plugin.getSelectionManager().setPos2(player.getUniqueId(), location);
        plugin.getMessages().send(sender, Message.POS2_SET, "position", location.toString());
    }
}
