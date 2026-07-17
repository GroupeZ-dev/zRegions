package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Adds a player to a region as owner or member (defaults to member).
 */
public class AddMemberCommand extends RegionCommand {

    public AddMemberCommand() {
        super("addmember", "zregions.admin", "addmember <region> <player> [owner|member]", "addmember");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalRegion = args.getOpt(1);
        Optional<String> optionalPlayer = args.getOpt(2);
        if (optionalRegion.isEmpty() || optionalPlayer.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> region = resolveRegion(plugin, sender, optionalRegion.get());
        if (region.isEmpty()) {
            return;
        }

        Optional<UUID> uniqueId = plugin.getBootstrap().lookupUniqueId(optionalPlayer.get());
        if (uniqueId.isEmpty()) {
            plugin.getMessages().send(sender, Message.PLAYER_NOT_FOUND, "player", optionalPlayer.get());
            return;
        }

        MemberRole role = MemberRole.MEMBER;
        Optional<String> optionalRole = args.getOpt(3);
        if (optionalRole.isPresent()) {
            String input = optionalRole.get();
            if (!input.equalsIgnoreCase("owner") && !input.equalsIgnoreCase("member")) {
                plugin.getMessages().send(sender, Message.MEMBER_INVALID_ROLE, "role", input);
                return;
            }
            role = MemberRole.valueOf(input.toUpperCase(Locale.ROOT));
        }

        plugin.getRegionManager().setMember(region.get(), uniqueId.get(), role);
        plugin.getMessages().send(sender, Message.MEMBER_ADDED,
                "player", optionalPlayer.get(),
                "role", role.name().toLowerCase(Locale.ROOT),
                "region", region.get().getName());
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, CompletionSupplier.startsWith(() -> plugin.getBootstrap().getPlayerList().stream()))
                .at(3, CompletionSupplier.startsWith("owner", "member"))
                .complete(args);
    }
}
