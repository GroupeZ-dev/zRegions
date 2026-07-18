package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A comma-separated list of strings (e.g. {@code command-blacklist}). The
 * separator is a comma because the flag command joins its value arguments with
 * single spaces — a space separator would be lossy. Entries are trimmed and
 * empty ones dropped; an input with no entry at all is invalid (unset the flag
 * instead). Parsed lists are immutable: values are read concurrently from the
 * region's flag map.
 */
public final class StringListFlag implements Flag<List<String>> {

    private final String key;
    private final List<String> defaultValue;

    public StringListFlag(String key, List<String> defaultValue) {
        this.key = Objects.requireNonNull(key, "key");
        this.defaultValue = List.copyOf(Objects.requireNonNull(defaultValue, "defaultValue"));
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public List<String> getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Optional<List<String>> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        List<String> entries = new ArrayList<>();
        for (String entry : input.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(entries));
    }

    @Override
    public String serialize(List<String> value) {
        return String.join(", ", value);
    }

    @Override
    public String toString() {
        return "StringListFlag{" + this.key + "}";
    }
}
