package fr.maxlego08.zregions.common.command;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.commands.AddMemberCommand;
import fr.maxlego08.zregions.common.command.commands.CreateCommand;
import fr.maxlego08.zregions.common.command.commands.FlagCommand;
import fr.maxlego08.zregions.common.command.commands.HelpCommand;
import fr.maxlego08.zregions.common.command.commands.InfoCommand;
import fr.maxlego08.zregions.common.command.commands.ListCommand;
import fr.maxlego08.zregions.common.command.commands.Pos1Command;
import fr.maxlego08.zregions.common.command.commands.Pos2Command;
import fr.maxlego08.zregions.common.command.commands.RedefineCommand;
import fr.maxlego08.zregions.common.command.commands.ReloadCommand;
import fr.maxlego08.zregions.common.command.commands.RemoveCommand;
import fr.maxlego08.zregions.common.command.commands.RemoveMemberCommand;
import fr.maxlego08.zregions.common.command.commands.SetParentCommand;
import fr.maxlego08.zregions.common.command.commands.SetPriorityCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Root manager for the {@code /rg} command (LuckPerms' CommandManager model).
 * Platforms convert their native sender/arguments and delegate here; execution runs
 * on the async pool so a slow storage call never blocks the game thread.
 */
public class RegionCommandManager {

    private final ZRegionsPlugin plugin;
    private final Map<String, RegionCommand> commands = new LinkedHashMap<>();

    public RegionCommandManager(ZRegionsPlugin plugin) {
        this.plugin = plugin;
        register(new HelpCommand());
        register(new Pos1Command());
        register(new Pos2Command());
        register(new CreateCommand());
        register(new RemoveCommand());
        register(new ListCommand());
        register(new InfoCommand());
        register(new FlagCommand());
        register(new AddMemberCommand());
        register(new RemoveMemberCommand());
        register(new SetPriorityCommand());
        register(new SetParentCommand());
        register(new RedefineCommand());
        register(new ReloadCommand());
    }

    private void register(RegionCommand command) {
        this.commands.put(command.getName().toLowerCase(Locale.ROOT), command);
    }

    public ZRegionsPlugin getPlugin() {
        return this.plugin;
    }

    /** The registered sub-commands, in registration (help display) order. */
    public Collection<RegionCommand> getCommands() {
        return Collections.unmodifiableCollection(this.commands.values());
    }

    public void executeCommand(RegionSender sender, String label, List<String> args) {
        boolean empty = args.isEmpty() || args.get(0).trim().isEmpty();
        String name = empty ? "help" : args.get(0).toLowerCase(Locale.ROOT);

        RegionCommand command = this.commands.get(name);
        if (command == null) {
            this.plugin.getMessages().send(sender, Message.UNKNOWN_COMMAND);
            return;
        }
        if (!command.isAuthorized(sender)) {
            this.plugin.getMessages().send(sender, Message.NO_PERMISSION);
            return;
        }

        ArgumentList arguments = new ArgumentList(args);
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            try {
                command.execute(this.plugin, sender, arguments);
            } catch (Throwable throwable) {
                this.plugin.getLogger().severe(
                        "Exception whilst executing command: /" + label + " " + String.join(" ", arguments), throwable);
            }
        });
    }

    public List<String> tabCompleteCommand(RegionSender sender, List<String> args) {
        List<RegionCommand> allowed = this.commands.values().stream()
                .filter(command -> command.isAuthorized(sender))
                .toList();

        return TabCompleter.create()
                .at(0, CompletionSupplier.startsWith(() -> allowed.stream().map(RegionCommand::getName)))
                .from(1, partial -> allowed.stream()
                        .filter(command -> command.getName().equalsIgnoreCase(args.get(0)))
                        .findFirst()
                        .map(command -> command.tabComplete(this.plugin, sender, new ArgumentList(args)))
                        .orElse(List.of()))
                .complete(args);
    }
}
