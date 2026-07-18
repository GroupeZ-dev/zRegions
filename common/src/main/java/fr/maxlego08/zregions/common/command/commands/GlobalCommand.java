package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;

/**
 * Get-or-create of a world's global region: a shapeless region whose flags apply
 * everywhere in the world after every positional lookup. Without argument the
 * player's world is targeted; the console must name one. Once created, it is
 * managed like any region ({@code /rg flag __global__ …}, {@code /rg remove …}).
 */
public class GlobalCommand extends RegionCommand {

    public GlobalCommand() {
        super("global", "zregions.admin", "global [world]", "global");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalWorld = args.getOpt(1);
        String worldName;
        if (optionalWorld.isPresent()) {
            worldName = optionalWorld.get();
        } else {
            Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
            if (optionalPlayer.isEmpty()) {
                sendUsage(plugin, sender);
                return;
            }
            worldName = optionalPlayer.get().getWorldName();
        }

        RegionManager manager = plugin.getRegionManager();
        Optional<Region> existing = manager.getGlobalRegion(worldName);
        if (existing.isPresent()) {
            plugin.getMessages().send(sender, Message.REGION_GLOBAL_EXISTS,
                    "world", worldName,
                    "region", existing.get().getName());
            return;
        }

        try {
            Region region = manager.createGlobalRegion(worldName);
            plugin.getMessages().send(sender, Message.REGION_GLOBAL_CREATED,
                    "world", worldName,
                    "region", region.getName());
        } catch (IllegalArgumentException exception) {
            // a normal region squats the reserved name in this world
            plugin.getMessages().send(sender, Message.REGION_ALREADY_EXISTS,
                    "region", RegionManager.GLOBAL_REGION_NAME);
        }
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, CompletionSupplier.startsWith(() -> plugin.getRegionManager().getRegions().stream()
                        .map(Region::getWorldName)
                        .distinct()))
                .complete(args);
    }
}
