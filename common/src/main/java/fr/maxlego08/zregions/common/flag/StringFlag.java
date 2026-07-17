package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;

import java.util.Objects;
import java.util.Optional;

/**
 * A free-text flag (MiniMessage allowed), used for zone messages such as
 * {@code greeting}/{@code farewell}. An empty value means "nothing to send";
 * unset the flag rather than storing an empty string.
 */
public final class StringFlag implements Flag<String> {

    private final String key;
    private final String defaultValue;

    public StringFlag(String key, String defaultValue) {
        this.key = Objects.requireNonNull(key, "key");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Optional<String> parse(String input) {
        return Optional.ofNullable(input);
    }

    @Override
    public String serialize(String value) {
        return value;
    }

    @Override
    public String toString() {
        return "StringFlag{" + this.key + "}";
    }
}
