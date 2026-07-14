package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * A boolean allow/deny flag (the WorldGuard "state" model). Accepts
 * {@code allow}/{@code true}/{@code on} and {@code deny}/{@code false}/{@code off}
 * as inputs, case-insensitively; serializes to {@code allow}/{@code deny}.
 */
public final class StateFlag implements Flag<Boolean> {

    private final String key;
    private final boolean defaultValue;

    public StateFlag(String key, boolean defaultValue) {
        this.key = Objects.requireNonNull(key, "key");
        this.defaultValue = defaultValue;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public Boolean getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Optional<Boolean> parse(String input) {
        if (input == null) return Optional.empty();
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "allow", "true", "on" -> Optional.of(Boolean.TRUE);
            case "deny", "false", "off" -> Optional.of(Boolean.FALSE);
            default -> Optional.empty();
        };
    }

    @Override
    public String serialize(Boolean value) {
        return value ? "allow" : "deny";
    }

    @Override
    public String toString() {
        return "StateFlag{" + this.key + "}";
    }
}
