package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Sets or unsets a flag value on a region, for an optional group target given as
 * a trailing {@code -t <target>} pair (defaults to ALL). Only the TRAILING pair is
 * an option, so string-flag values may contain a literal "-t". The value may span
 * several arguments; {@code unset} (or {@code -}) removes the value.
 */
public class FlagCommand extends RegionCommand {

    private static final List<String> VALUE_SUGGESTIONS = List.of("allow", "deny", "true", "false", "unset");

    public FlagCommand() {
        super("flag", "zregions.admin", "flag <region> <flag> <value...|unset> [-t <target>]", "flag");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalRegion = args.getOpt(1);
        Optional<String> optionalFlag = args.getOpt(2);
        if (optionalRegion.isEmpty() || optionalFlag.isEmpty() || args.getOpt(3).isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> region = resolveRegion(plugin, sender, optionalRegion.get());
        if (region.isEmpty()) {
            return;
        }

        Optional<Flag<?>> flag = plugin.getFlagRegistry().getFlag(optionalFlag.get());
        if (flag.isEmpty()) {
            plugin.getMessages().send(sender, Message.FLAG_UNKNOWN, "flag", optionalFlag.get());
            return;
        }

        // A bare trailing "-t" means a target was intended but missing.
        if (args.get(args.size() - 1).equals("-t")) {
            sendUsage(plugin, sender);
            return;
        }

        // Only the TRAILING "-t <target>" pair is an option; an invalid target aborts
        // (silently defaulting to ALL would apply the flag to everyone by surprise).
        GroupTarget target = GroupTarget.ALL;
        int valueEnd = args.size();
        if (args.size() >= 5 && args.get(args.size() - 2).equals("-t")) {
            String rawTarget = args.get(args.size() - 1);
            try {
                target = GroupTarget.valueOf(rawTarget.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                plugin.getMessages().send(sender, Message.FLAG_TARGET_INVALID, "target", rawTarget);
                return;
            }
            valueEnd = args.size() - 2;
        }

        String value = String.join(" ", args.subList(3, valueEnd));
        if (value.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        if (value.equalsIgnoreCase("unset") || value.equals("-")) {
            plugin.getRegionManager().removeFlag(region.get(), flag.get(), target);
            plugin.getMessages().send(sender, Message.FLAG_UNSET,
                    "flag", flag.get().getKey(),
                    "target", target.name(),
                    "region", region.get().getName());
            return;
        }

        Optional<String> applied = apply(plugin, region.get(), flag.get(), target, value);
        if (applied.isEmpty()) {
            plugin.getMessages().send(sender, Message.FLAG_VALUE_INVALID,
                    "value", value,
                    "flag", flag.get().getKey());
            return;
        }

        plugin.getMessages().send(sender, Message.FLAG_SET,
                "flag", flag.get().getKey(),
                "value", applied.get(),
                "target", target.name(),
                "region", region.get().getName());
    }

    /**
     * Captures the flag's value type {@code T} so the parse result can be fed back
     * into {@link RegionManager#setFlag}; returns the serialized value on success.
     */
    private <T> Optional<String> apply(ZRegionsPlugin plugin, Region region, Flag<T> flag, GroupTarget target, String input) {
        Optional<T> value = flag.parse(input);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        plugin.getRegionManager().setFlag(region, flag, target, value.get());
        return Optional.of(flag.serialize(value.get()));
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        // Context-sensitive tail matching the trailing-pair grammar: right after
        // "-t" suggest the targets, anywhere else in the value suggest "-t".
        int lastIndex = args.size() - 1;
        if (lastIndex >= 4 && args.get(lastIndex - 1).equals("-t")) {
            return CompletionSupplier.startsWith(() -> Arrays.stream(GroupTarget.values())
                            .map(target -> target.name().toLowerCase(Locale.ROOT)))
                    .supplyCompletions(args.get(lastIndex));
        }
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, CompletionSupplier.startsWith(() -> plugin.getFlagRegistry().getFlags().stream().map(Flag::getKey)))
                .at(3, CompletionSupplier.startsWith(VALUE_SUGGESTIONS))
                .from(4, CompletionSupplier.startsWith("-t"))
                .complete(args);
    }
}
