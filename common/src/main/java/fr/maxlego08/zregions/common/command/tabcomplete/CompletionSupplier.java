/*
 * Ported from LuckPerms (https://github.com/LuckPerms/LuckPerms), MIT License.
 * Copyright (c) lucko (Luck) <luck@lucko.me>, Copyright (c) contributors
 */
package fr.maxlego08.zregions.common.command.tabcomplete;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Supplies the tab completions for a partially typed argument.
 */
@FunctionalInterface
public interface CompletionSupplier {

    CompletionSupplier EMPTY = partial -> Collections.emptyList();

    static CompletionSupplier startsWith(String... strings) {
        return startsWith(() -> Arrays.stream(strings));
    }

    static CompletionSupplier startsWith(Collection<String> strings) {
        return startsWith(strings::stream);
    }

    static CompletionSupplier startsWith(Supplier<Stream<String>> strings) {
        return partial -> strings.get()
                .filter(string -> string.length() >= partial.length()
                        && string.regionMatches(true, 0, partial, 0, partial.length()))
                .collect(Collectors.toList());
    }

    List<String> supplyCompletions(String partial);
}
