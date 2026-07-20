package fr.maxlego08.zregions.bukkit.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a Brigadier greedy-string tail (the text after a {@code /region <sub>} literal)
 * into the argument list the common {@code RegionCommandManager} expects — the
 * sub-command name followed by its tokens. Shared by the Paper and commodore trees.
 */
public final class CommandArguments {

    private CommandArguments() {
    }

    /**
     * Arguments for EXECUTION under a sub-command literal: the sub-command name, then
     * the whitespace-separated tokens of {@code tail} (empty tail → just the name).
     */
    public static List<String> forExecution(String subCommand, String tail) {
        List<String> args = new ArrayList<>();
        args.add(subCommand);
        if (tail != null && !tail.isBlank()) {
            for (String token : tail.trim().split(" +")) {
                args.add(token);
            }
        }
        return args;
    }

    /**
     * Arguments for COMPLETION under a sub-command literal: the sub-command name, then
     * the space-split tokens of {@code tail} keeping the trailing empty token — so a
     * trailing space completes the next (empty) argument, mirroring Bukkit's split.
     */
    public static List<String> forCompletion(String subCommand, String tail) {
        List<String> args = new ArrayList<>();
        args.add(subCommand);
        for (String token : tail.split(" ", -1)) {
            args.add(token);
        }
        return args;
    }
}
