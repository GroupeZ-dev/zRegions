package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.BoundingBox;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Teleports the player to a region: the {@code teleport} location flag if set,
 * otherwise a safe standable spot in the bounding-box centre column. The global
 * region has no shape to target and is refused, and the {@code spawn-teleport}
 * flag can forbid non-bypass players from teleporting to a given region.
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
        UUID playerId = player.getUniqueId();
        boolean bypass = player.hasPermission(plugin.getConfiguration().getBypassPermission());
        // spawn-teleport gates player access to /rg teleport per region; admins bypass it
        if (!bypass && !plugin.getRegionManager().resolveFlag(region, Flags.SPAWN_TELEPORT, playerId)) {
            plugin.getMessages().send(sender, Message.REGION_TELEPORT_DENIED, "region", region.getName());
            return;
        }
        BoundingBox box = region.getShape().getBoundingBox();

        // the world read and the teleport run on the game thread (commands run async)
        plugin.getBootstrap().getScheduler().executeSync(() -> {
            // a custom teleport-location flag overrides the bounding-box centre (used as-is)
            Optional<RegionLocation> custom = plugin.getRegionManager()
                    .resolveFlagIfSet(region, Flags.TELEPORT, playerId);
            if (custom.isPresent()) {
                player.teleport(custom.get());
                sendArrivalMessage(plugin, sender, player, region, playerId);
                return;
            }
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
            sendArrivalMessage(plugin, sender, player, region, playerId);
        });
    }

    /** The region's {@code teleport-message} if set, otherwise the generic confirmation. */
    private void sendArrivalMessage(ZRegionsPlugin plugin, RegionSender sender, RegionPlayer player,
                                    Region region, UUID playerId) {
        String custom = plugin.getRegionManager().resolveFlag(region, Flags.TELEPORT_MESSAGE, playerId);
        if (custom != null && !custom.isEmpty()) {
            sender.sendMessage(plugin.getMessages().formatRaw(custom,
                    "player", player.getName(), "region", region.getName()));
        } else {
            plugin.getMessages().send(sender, Message.REGION_TELEPORTED, "region", region.getName());
        }
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .complete(args);
    }
}
