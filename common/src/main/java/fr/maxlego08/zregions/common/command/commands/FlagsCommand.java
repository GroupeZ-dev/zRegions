package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Comparator;
import java.util.List;

/**
 * The flag catalogue: {@code /rg flags [page]} lists every registered flag with,
 * on hover, its localized description, and click-to-start a {@code /rg flag}
 * command. Read-only and paginated (addon flags appear too, as they are in the
 * registry). The description text lives in {@code flags.<key>} of the language file.
 */
public class FlagsCommand extends RegionCommand {

    private static final int PAGE_SIZE = 15;

    public FlagsCommand() {
        super("flags", "zregions.use", "flags [page]", "flags");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        MessageService messages = plugin.getMessages();
        List<Flag<?>> flags = plugin.getFlagRegistry().getFlags().stream()
                .sorted(Comparator.comparing(Flag::getKey))
                .toList();

        int pages = Math.max(1, (int) Math.ceil(flags.size() / (double) PAGE_SIZE));
        int page = Math.min(Math.max(1, args.getIntOrDefault(1, 1)), pages);
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, flags.size());

        messages.send(sender, Message.FLAG_LIST_HEADER, "count", String.valueOf(flags.size()));
        for (Flag<?> flag : flags.subList(from, to)) {
            sender.sendMessage(entry(messages, flag));
        }
        if (pages > 1) {
            sender.sendMessage(pageFooter(plugin, "flags", page, pages));
        }
    }

    private Component entry(MessageService messages, Flag<?> flag) {
        String key = flag.getKey();
        Component hover = messages.format(Message.FLAG_LIST_HOVER,
                "flag", key,
                "description", messages.flagDescription(key));
        return messages.format(Message.FLAG_LIST_ENTRY, "flag", key, "type", type(flag))
                // suggest (not run): the catalogue seeds the command, the admin fills in the region/value
                .clickEvent(ClickEvent.suggestCommand("/rg flag "))
                .hoverEvent(HoverEvent.showText(hover));
    }

    /** The value kind shown next to each flag: state / number / list / text. */
    private static String type(Flag<?> flag) {
        Object def = flag.getDefaultValue();
        if (def instanceof Boolean) return "state";
        if (def instanceof Number) return "number";
        if (def instanceof List<?>) return "list";
        return "text";
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return List.of();
    }
}
