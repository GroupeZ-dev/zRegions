package fr.maxlego08.zregions.common.command.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A list of string arguments with parsing helpers (LuckPerms' ArgumentList model).
 * The sub-command name sits at index 0; command parameters start at index 1.
 */
public class ArgumentList extends ArrayList<String> {

    public ArgumentList(List<String> arguments) {
        super(arguments);
    }

    public boolean indexOutOfBounds(int index) {
        return index < 0 || index >= size();
    }

    /** The argument at {@code index}, or empty when out of bounds. */
    public Optional<String> getOpt(int index) {
        return indexOutOfBounds(index) ? Optional.empty() : Optional.of(get(index));
    }

    /** The argument at {@code index} parsed as an int, or {@code def} when absent/invalid. */
    public int getIntOrDefault(int index, int def) {
        if (indexOutOfBounds(index)) {
            return def;
        }
        try {
            return Integer.parseInt(get(index));
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    /** Joins the arguments from {@code fromIndex} (inclusive) with spaces; empty when out of range. */
    public String join(int fromIndex) {
        if (fromIndex >= size()) {
            return "";
        }
        return String.join(" ", subList(Math.max(0, fromIndex), size()));
    }
}
