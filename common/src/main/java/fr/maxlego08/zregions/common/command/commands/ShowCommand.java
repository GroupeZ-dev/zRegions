package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
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
 * Outlines a region's borders with particles visible to the executing player
 * only: by name, or — without argument — the highest-priority region at the
 * player's position. An optional duration (seconds) overrides the configured
 * default; running the command again replaces the previous outline.
 *
 * <p>Grammar note: with a single argument, a plain number matching NO region
 * name is read as the duration for the region at the player's position, so
 * {@code /rg show 30} works — a region actually named "30" still wins.</p>
 */
public class ShowCommand extends RegionCommand {

    public ShowCommand() {
        super("show", "zregions.use", "show [name] [seconds]", "show");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
        if (optionalPlayer.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_ONLY);
            return;
        }
        RegionPlayer player = optionalPlayer.get();

        Optional<String> firstArgument = args.getOpt(1);
        Optional<String> secondArgument = args.getOpt(2);

        String name = null;
        int requestedSeconds = 0; // <= 0 means "use the configured default"
        if (secondArgument.isPresent()) {
            name = firstArgument.get();
            Optional<Integer> seconds = parseSeconds(secondArgument.get());
            if (seconds.isEmpty()) {
                sendUsage(plugin, sender);
                return;
            }
            requestedSeconds = seconds.get();
        } else if (firstArgument.isPresent()) {
            String value = firstArgument.get();
            Optional<Integer> seconds = parseSeconds(value);
            if (seconds.isPresent() && findRegion(plugin, value).isEmpty()) {
                requestedSeconds = seconds.get();
            } else {
                name = value;
            }
        }

        Optional<Region> optionalRegion;
        if (name != null) {
            optionalRegion = resolveRegion(plugin, sender, name);
            if (optionalRegion.isEmpty()) {
                return;
            }
        } else {
            RegionLocation location = player.getLocation();
            optionalRegion = plugin.getRegionManager().getHighestRegionAt(
                    location.getWorldName(), location.getX(), location.getY(), location.getZ());
            if (optionalRegion.isEmpty()) {
                plugin.getMessages().send(sender, Message.REGION_NOT_FOUND, "region", "here");
                return;
            }
        }

        Region region = optionalRegion.get();
        if (region.isGlobal()) {
            plugin.getMessages().send(sender, Message.BORDER_GLOBAL);
            return;
        }
        // particles spawn in the player's own world — the coordinates only make sense there
        if (!region.getWorldName().equals(player.getWorldName())) {
            plugin.getMessages().send(sender, Message.BORDER_OTHER_WORLD,
                    "region", region.getName(),
                    "region_world", region.getWorldName(),
                    "world", player.getWorldName());
            return;
        }

        int seconds = plugin.getBorderDisplay().show(player, region, requestedSeconds);
        plugin.getMessages().send(sender, Message.BORDER_SHOWN,
                "region", region.getName(),
                "seconds", String.valueOf(seconds));
    }

    /** A strictly positive number of seconds, or empty. */
    private static Optional<Integer> parseSeconds(String input) {
        try {
            int seconds = Integer.parseInt(input);
            return seconds > 0 ? Optional.of(seconds) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, CompletionSupplier.startsWith("5", "10", "30", "60"))
                .complete(args);
    }
}
