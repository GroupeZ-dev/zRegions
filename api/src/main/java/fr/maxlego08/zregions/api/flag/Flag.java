package fr.maxlego08.zregions.api.flag;

import java.util.Optional;

/**
 * A typed region flag. Flags are registered in the {@link FlagRegistry}; addons can
 * register their own (the extension model UltraRegions used for custom flags).
 *
 * @param <T> the value type
 */
public interface Flag<T> {

    /** Unique kebab-case key, e.g. {@code "block-break"}. */
    String getKey();

    /** The value applied when no region defines this flag. */
    T getDefaultValue();

    /** Parses a user/storage input into a value; empty when the input is invalid. */
    Optional<T> parse(String input);

    /** Serializes a value to its storage/command representation. */
    String serialize(T value);
}
