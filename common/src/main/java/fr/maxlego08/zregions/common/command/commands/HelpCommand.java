package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;

/**
 * Lists the sub-commands the sender is allowed to use, paginated ({@code /rg help
 * [page]}). Each entry is clickable: clicking it inserts the command in the chat
 * box (suggest), and hovering explains that — the interactivity is attached in Java
 * so the command text never has to be escaped into a MiniMessage tag argument.
 */
public class HelpCommand extends RegionCommand {

    private static final int PAGE_SIZE = 8;

    public HelpCommand() {
        super("help", "zregions.use", "help [page]", "help");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        MessageService messages = plugin.getMessages();
        List<RegionCommand> commands = plugin.getCommandManager().getCommands().stream()
                .filter(command -> command.isAuthorized(sender))
                .toList();

        int pages = Math.max(1, (int) Math.ceil(commands.size() / (double) PAGE_SIZE));
        int page = Math.min(Math.max(1, args.getIntOrDefault(1, 1)), pages);
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, commands.size());

        messages.send(sender, Message.HELP_HEADER);
        Component hover = messages.format(Message.HELP_HOVER);
        for (RegionCommand command : commands.subList(from, to)) {
            Component line = messages.format(Message.HELP_ENTRY,
                            "usage", command.getUsage(),
                            "description", command.getDescription(plugin))
                    .clickEvent(ClickEvent.suggestCommand("/rg " + command.getName() + " "))
                    .hoverEvent(HoverEvent.showText(hover));
            sender.sendMessage(line);
        }
        if (pages > 1) {
            sender.sendMessage(pageFooter(plugin, "help", page, pages));
        }
    }
}
