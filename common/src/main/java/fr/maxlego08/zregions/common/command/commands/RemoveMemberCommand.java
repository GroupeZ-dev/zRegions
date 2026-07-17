package fr.maxlego08.zregions.common.command.commands;

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
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Removes a player from a region's member list.
 */
public class RemoveMemberCommand extends RegionCommand {

    public RemoveMemberCommand() {
        super("removemember", "zregions.admin", "removemember <region> <player>", "removemember");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalRegion = args.getOpt(1);
        Optional<String> optionalPlayer = args.getOpt(2);
        if (optionalRegion.isEmpty() || optionalPlayer.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> optionalFound = resolveRegion(plugin, sender, optionalRegion.get());
        if (optionalFound.isEmpty()) {
            return;
        }

        Optional<UUID> uniqueId = plugin.getBootstrap().lookupUniqueId(optionalPlayer.get());
        if (uniqueId.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_NOT_FOUND, "player", optionalPlayer.get());
            return;
        }

        Region region = optionalFound.get();
        UUID playerId = uniqueId.get();
        boolean isMember = region.getMembers().stream()
                .anyMatch(member -> member.getPlayerId().equals(playerId));
        if (!isMember) {
            plugin.getMessages().send(sender, Message.MEMBER_NOT_MEMBER,
                    "player", optionalPlayer.get(),
                    "region", region.getName());
            return;
        }

        plugin.getRegionManager().removeMember(region, playerId);
        plugin.getMessages().send(sender, Message.MEMBER_REMOVED,
                "player", optionalPlayer.get(),
                "region", region.getName());
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        // Online members only: lookupUsername would read playerdata from disk on
        // the main thread for every offline member, on every keystroke.
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, CompletionSupplier.startsWith(() -> args.getOpt(1)
                        .flatMap(name -> findRegion(plugin, name))
                        .map(region -> region.getMembers().stream()
                                .flatMap(member -> plugin.getBootstrap().getPlayer(member.getPlayerId())
                                        .map(RegionPlayer::getName).stream()))
                        .orElseGet(Stream::empty)))
                .complete(args);
    }
}
