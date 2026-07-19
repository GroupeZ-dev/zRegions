package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;
import java.util.Optional;

/**
 * Deletes a region, looked up by name across all worlds. Deletion is
 * irreversible, so it is always guarded by a confirmation: {@code /rg remove
 * <name>} shows a clickable [Confirm] button that runs {@code /rg remove <name>
 * confirm}, which actually performs the deletion. The two-step is stateless (the
 * token lives in the command), so it survives the async command pool with no expiry.
 */
public class RemoveCommand extends RegionCommand {

    public RemoveCommand() {
        super("remove", "zregions.admin", "remove <name>", "remove");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalName = args.getOpt(1);
        if (optionalName.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> optionalRegion = resolveRegion(plugin, sender, optionalName.get());
        if (optionalRegion.isEmpty()) {
            return;
        }

        Region region = optionalRegion.get();
        boolean confirmed = args.getOpt(2).map(arg -> arg.equalsIgnoreCase("confirm")).orElse(false);
        if (!confirmed) {
            sendConfirmation(plugin, sender, region);
            return;
        }

        plugin.getRegionManager().deleteRegion(region);
        plugin.getMessages().send(sender, Message.REGION_REMOVED, "region", region.getName());
    }

    /** Interactive confirmation: the [Confirm] button re-runs this command with the token. */
    private void sendConfirmation(ZRegionsPlugin plugin, RegionSender sender, Region region) {
        MessageService messages = plugin.getMessages();
        // target the region by UUID: unambiguous and space/colon-free, so the click
        // command survives Bukkit's space-splitting even in worlds with spaced names
        Component button = messages.format(Message.REGION_REMOVE_CONFIRM_BUTTON)
                .clickEvent(ClickEvent.runCommand("/rg remove " + region.getId() + " confirm"))
                .hoverEvent(HoverEvent.showText(messages.format(Message.REGION_REMOVE_CONFIRM_HOVER)));
        Component prompt = messages.format(Message.REGION_REMOVE_CONFIRM, "region", region.getName())
                .append(button)
                .append(messages.format(Message.REGION_REMOVE_CONFIRM_HINT));
        sender.sendMessage(prompt);
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, CompletionSupplier.startsWith("confirm"))
                .complete(args);
    }
}
