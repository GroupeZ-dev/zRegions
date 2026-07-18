package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.selection.StarGenerator;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;

/**
 * Fills the selection's polygon vertex list with a star centered on the player
 * (the product's signature shape — plan §7): {@code 2 × branches} vertices
 * alternating between the outer and the inner radius (defaults to half the
 * outer one). Finish with {@code /rg pos1}/{@code pos2} for the height and
 * {@code /rg create <name> polygon}.
 */
public class StarCommand extends RegionCommand {

    public StarCommand() {
        super("star", "zregions.admin", "star <branches> <outerRadius> [innerRadius]", "star");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }
        Optional<String> optionalBranches = args.getOpt(1);
        Optional<String> optionalOuter = args.getOpt(2);
        if (optionalBranches.isEmpty() || optionalOuter.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        int branches;
        double outerRadius;
        double innerRadius;
        try {
            branches = Integer.parseInt(optionalBranches.get());
            outerRadius = Double.parseDouble(optionalOuter.get());
            innerRadius = args.getOpt(3).map(Double::parseDouble).orElse(outerRadius / 2);
        } catch (NumberFormatException exception) {
            sendUsage(plugin, sender);
            return;
        }

        RegionPlayer player = optionalPlayer.get();
        RegionLocation center = player.getLocation();
        List<RegionLocation> vertices;
        try {
            vertices = StarGenerator.vertices(center.getWorldName(), center.getX(), center.getY(),
                    center.getZ(), branches, outerRadius, innerRadius);
        } catch (IllegalArgumentException exception) {
            sendUsage(plugin, sender);
            return;
        }

        plugin.getSelectionManager().setPoints(player.getUniqueId(), vertices);
        plugin.getMessages().send(sender, Message.STAR_GENERATED,
                "branches", String.valueOf(branches),
                "points", String.valueOf(vertices.size()));
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, CompletionSupplier.startsWith("4", "5", "6", "8"))
                .at(2, CompletionSupplier.startsWith("10", "20", "30"))
                .complete(args);
    }
}
