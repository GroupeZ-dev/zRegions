package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.region.ZRegionManager;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Sets a flag value on a region, for an optional group target (defaults to ALL).
 */
public class FlagCommand extends RegionCommand {

    private static final List<String> VALUE_SUGGESTIONS = List.of("allow", "deny", "true", "false");

    public FlagCommand() {
        super("flag", "zregions.admin", "flag <region> <flag> <value> [target]", "flag");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalRegion = args.getOpt(1);
        Optional<String> optionalFlag = args.getOpt(2);
        Optional<String> optionalValue = args.getOpt(3);
        if (optionalRegion.isEmpty() || optionalFlag.isEmpty() || optionalValue.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }

        Optional<Region> region = findRegion(plugin, optionalRegion.get());
        if (region.isEmpty()) {
            plugin.getMessages().send(sender, Message.REGION_NOT_FOUND, "region", optionalRegion.get());
            return;
        }

        Optional<Flag<?>> flag = plugin.getFlagRegistry().getFlag(optionalFlag.get());
        if (flag.isEmpty()) {
            plugin.getMessages().send(sender, Message.FLAG_UNKNOWN, "flag", optionalFlag.get());
            return;
        }

        GroupTarget target = GroupTarget.parse(args.getOpt(4).orElse(null), GroupTarget.ALL);
        Optional<String> applied = apply(plugin, region.get(), flag.get(), target, optionalValue.get());
        if (applied.isEmpty()) {
            plugin.getMessages().send(sender, Message.FLAG_VALUE_INVALID,
                    "value", optionalValue.get(),
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
     * into {@link ZRegionManager#setFlag}; returns the serialized value on success.
     */
    private <T> Optional<String> apply(ZRegionsPlugin plugin, Region region, Flag<T> flag, GroupTarget target, String input) {
        Optional<T> value = flag.parse(input);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        ((ZRegionManager) plugin.getRegionManager()).setFlag(region, flag, target, value.get());
        return Optional.of(flag.serialize(value.get()));
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, regionNames(plugin))
                .at(2, CompletionSupplier.startsWith(() -> plugin.getFlagRegistry().getFlags().stream().map(Flag::getKey)))
                .at(3, CompletionSupplier.startsWith(VALUE_SUGGESTIONS))
                .at(4, CompletionSupplier.startsWith(() -> Arrays.stream(GroupTarget.values())
                        .map(target -> target.name().toLowerCase(Locale.ROOT))))
                .complete(args);
    }
}
