package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;

/**
 * Teleports the player to a region: a safe standable spot in the bounding-box
 * centre column. The global region has no shape to target and is refused.
 *
 * <p>Commands execute on the async pool, but the safe-spot search reads the world
 * and the teleport moves the player — both must run on the game thread, so they
 * are scheduled through the platform's sync executor.</p>
 */
public class TeleportCommand extends RegionCommand {

    public TeleportCommand() {
        super("teleport", "zregions.teleport", "teleport <region>", "teleport", List.of("tp"));
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }
        Optional<String> name = args.getOpt(1);
        if (name.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }
        Optional<Region> optionalRegion = resolveRegion(plugin, sender, name.get());
        if (optionalRegion.isEmpty()) {
            return;
        }
        Region region = optionalRegion.get();
        if (region.getShape() == null) {
            plugin.getMessages().send(sender, Message.REGION_TELEPORT_GLOBAL);
            return;
        }

        RegionPlayer player = optionalPlayer.get();
        BoundingBox box = region.getShape().getBoundingBox();

        // the world read and the teleport run on the game thread (commands run async)
        plugin.getBootstrap().getScheduler().executeSync(() -> {
            RegionLocation centre = new RegionLocation(region.getWorldName(),
                    (box.minX() + box.maxX()) / 2.0,
                    (box.minY() + box.maxY()) / 2.0,
                    (box.minZ() + box.maxZ()) / 2.0,
                    player.getLocation().getYaw(), player.getLocation().getPitch());
            Optional<RegionLocation> safe = player.findSafeSpot(centre);
            if (safe.isEmpty()) {
                plugin.getMessages().send(sender, Message.REGION_TELEPORT_NO_SAFE, "region", region.getName());
                return;
            }
            player.teleport(safe.get());
            plugin.getMessages().send(sender, Message.REGION_TELEPORTED, "region", region.getName());
        });
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .complete(args);
    }
}
