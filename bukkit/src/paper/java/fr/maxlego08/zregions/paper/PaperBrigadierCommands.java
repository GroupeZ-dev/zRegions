package fr.maxlego08.zregions.paper;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.bukkit.command.CommandArguments;
import fr.maxlego08.zregions.bukkit.command.PlatformCommands;
import fr.maxlego08.zregions.common.command.RegionCommandManager;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.sender.RegionSender;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Paper/Folia native Brigadier registration (isolated in the {@code paper} sourceSet,
 * loaded only through {@link PlatformCommands} after a Paper presence check). Registers
 * {@code /region} (alias {@code /rg}) and {@code /zregions} as a real Brigadier tree —
 * sub-command literals with native completion and inline argument suggestions — every
 * node delegating to the common {@code RegionCommandManager} for the actual work.
 *
 * <p>Model: {@code DialogWarps}' {@code command/Commands.java}, adapted to a single root
 * that fans out to the shared sub-command list instead of one node per command.</p>
 */
public final class PaperBrigadierCommands implements PlatformCommands {

    private final ZRegionsBukkitPlugin plugin;

    public PaperBrigadierCommands(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        JavaPlugin loader = this.plugin.getBootstrap().getLoader();
        loader.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(buildTree("region"), "Manage regions", List.of("rg"));
            registrar.register(buildTree("zregions"), "zRegions admin", List.of());
        });
    }

    private LiteralCommandNode<CommandSourceStack> buildTree(String rootName) {
        RegionCommandManager manager = this.plugin.getCommandManager();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(rootName)
                .requires(source -> source.getSender().hasPermission("zregions.use"))
                .executes(context -> execute(context, rootName, List.of()));

        for (RegionCommand sub : manager.getCommands()) {
            root.then(subCommand(rootName, sub, sub.getName()));
            for (String alias : sub.getAliases()) {
                root.then(subCommand(rootName, sub, alias));
            }
        }
        // Unrecognised first token: route to the manager so its own "unknown sub-command"
        // message shows instead of Brigadier's generic red error.
        root.then(Commands.argument("input", StringArgumentType.greedyString())
                .executes(context -> execute(context, rootName, tokens(StringArgumentType.getString(context, "input")))));
        return root.build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> subCommand(String rootName, RegionCommand sub, String literal) {
        return Commands.literal(literal)
                .requires(source -> sub.isAuthorized(wrap(source)))
                .executes(context -> execute(context, rootName, List.of(sub.getName())))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((context, builder) -> suggest(context, builder, sub.getName()))
                        .executes(context -> execute(context, rootName,
                                CommandArguments.forExecution(sub.getName(), StringArgumentType.getString(context, "args")))));
    }

    private int execute(CommandContext<CommandSourceStack> context, String label, List<String> args) {
        this.plugin.getCommandManager().executeCommand(wrap(context.getSource()), label, new ArrayList<>(args));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context,
                                                   SuggestionsBuilder builder, String subName) {
        RegionSender sender = wrap(context.getSource());
        String remaining = builder.getRemaining();
        List<String> args = CommandArguments.forCompletion(subName, remaining);
        List<String> completions = this.plugin.getCommandManager().tabCompleteCommand(sender, args);
        // suggestions replace only the current (last) token, not the whole greedy tail
        int lastSpace = remaining.lastIndexOf(' ');
        SuggestionsBuilder target = lastSpace < 0 ? builder : builder.createOffset(builder.getStart() + lastSpace + 1);
        for (String completion : completions) {
            target.suggest(completion);
        }
        return target.buildFuture();
    }

    private RegionSender wrap(CommandSourceStack source) {
        return this.plugin.getSenderFactory().wrap(source.getSender());
    }

    private static List<String> tokens(String input) {
        List<String> tokens = new ArrayList<>();
        if (input != null && !input.isBlank()) {
            for (String token : input.trim().split(" +")) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
