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
 * Appends the player's current position to the polygon vertex list of their
 * selection ({@code /rg create <name> polygon} consumes it).
 */
public class AddPointCommand extends RegionCommand {

    public AddPointCommand() {
        super("addpoint", "zregions.admin", "addpoint", "addpoint");
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
        int count = plugin.getSelectionManager().addPoint(player.getUniqueId(), location);
        plugin.getMessages().send(sender, Message.POINT_ADDED,
                "count", String.valueOf(count),
                "position", location.toString());
    }
}
