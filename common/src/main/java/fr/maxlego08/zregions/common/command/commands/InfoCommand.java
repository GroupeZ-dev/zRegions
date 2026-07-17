package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shows the details of a region: by name, or — for a player without argument —
 * the highest priority region at their position.
 */
public class InfoCommand extends RegionCommand {

    public InfoCommand() {
        super("info", "zregions.use", "info [name]", "info");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalName = args.getOpt(1);

        Optional<Region> optionalRegion;
        if (optionalName.isPresent()) {
            optionalRegion = resolveRegion(plugin, sender, optionalName.get());
            if (optionalRegion.isEmpty()) {
                return;
            }
        } else {
            Optional<RegionPlayer> optionalPlayer = sender.asPlayer();
            if (optionalPlayer.isEmpty()) {
                plugin.getMessages().send(sender, Message.PLAYER_ONLY);
                return;
            }
            RegionLocation location = optionalPlayer.get().getLocation();
            optionalRegion = plugin.getRegionManager().getHighestRegionAt(
                    location.getWorldName(), location.getX(), location.getY(), location.getZ());
        }

        if (optionalRegion.isEmpty()) {
            plugin.getMessages().send(sender, Message.REGION_NOT_FOUND, "region", optionalName.orElse("here"));
            return;
        }

        Region region = optionalRegion.get();
        MessageService messages = plugin.getMessages();
        messages.send(sender, Message.REGION_INFO_HEADER, "region", region.getName());
        messages.send(sender, Message.REGION_INFO_WORLD, "world", region.getWorldName());
        messages.send(sender, Message.REGION_INFO_SHAPE, "shape", region.getShape().getType().name());
        messages.send(sender, Message.REGION_INFO_PRIORITY, "priority", String.valueOf(region.getPriority()));
        messages.send(sender, Message.REGION_INFO_MEMBERS, "members", formatMembers(plugin, region));
        messages.send(sender, Message.REGION_INFO_FLAGS, "flags", formatFlags(plugin, region));
    }

    private String formatMembers(ZRegionsPlugin plugin, Region region) {
        String members = region.getMembers().stream()
                .map(member -> plugin.getBootstrap().lookupUsername(member.getPlayerId())
                        .orElseGet(() -> member.getPlayerId().toString()) + "(" + member.getRole().name() + ")")
                .collect(Collectors.joining(", "));
        return members.isEmpty() ? "-" : members;
    }

    private String formatFlags(ZRegionsPlugin plugin, Region region) {
        String flags = plugin.getFlagRegistry().getFlags().stream()
                .map(flag -> formatFlag(region, flag))
                .flatMap(Optional::stream)
                .collect(Collectors.joining(", "));
        return flags.isEmpty() ? "-" : flags;
    }

    /** Captures the flag's value type so the raw value can be serialized back. */
    private <T> Optional<String> formatFlag(Region region, Flag<T> flag) {
        return region.getFlag(flag, GroupTarget.ALL).map(value -> flag.getKey() + "=" + flag.serialize(value));
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .complete(args);
    }
}
