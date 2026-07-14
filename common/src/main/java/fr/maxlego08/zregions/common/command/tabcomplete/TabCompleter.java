/*
 * Ported from LuckPerms (https://github.com/LuckPerms/LuckPerms), MIT License.
 * Copyright (c) lucko (Luck) <luck@lucko.me>, Copyright (c) contributors
 */
package fr.maxlego08.zregions.common.command.tabcomplete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Positional tab completion: a {@link CompletionSupplier} is bound to each argument
 * index with {@link #at(int, CompletionSupplier)}, or to an index and everything
 * after it with {@link #from(int, CompletionSupplier)}.
 */
public class TabCompleter {

    private final Map<Integer, CompletionSupplier> suppliers = new HashMap<>();
    private int from = Integer.MAX_VALUE;

    private TabCompleter() {
    }

    public static TabCompleter create() {
        return new TabCompleter();
    }

    /** Uses {@code supplier} to compute the completions at {@code position}. */
    public TabCompleter at(int position, CompletionSupplier supplier) {
        if (position >= this.from) {
            throw new IllegalStateException("at(" + position + ") called after from(" + this.from + ")");
        }
        this.suppliers.put(position, supplier);
        return this;
    }

    /** Uses {@code supplier} at {@code position} and every subsequent index. */
    public TabCompleter from(int position, CompletionSupplier supplier) {
        if (this.from != Integer.MAX_VALUE) {
            throw new IllegalStateException("from(...) can only be called once");
        }
        this.suppliers.put(position, supplier);
        this.from = position;
        return this;
    }

    public List<String> complete(List<String> args) {
        int lastIndex = 0;
        String partial;

        // nothing entered yet
        if (args.isEmpty() || (partial = args.get(lastIndex = args.size() - 1)).trim().isEmpty()) {
            return getCompletions(lastIndex, "");
        }

        // started typing something
        return getCompletions(lastIndex, partial);
    }

    private List<String> getCompletions(int position, String partial) {
        if (position >= this.from) {
            return this.suppliers.get(this.from).supplyCompletions(partial);
        }
        return this.suppliers.getOrDefault(position, CompletionSupplier.EMPTY).supplyCompletions(partial);
    }
}
