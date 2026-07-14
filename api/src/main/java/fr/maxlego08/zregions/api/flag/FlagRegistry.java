package fr.maxlego08.zregions.api.flag;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry of all known flags. Built-in flags are registered at startup;
 * addons may register additional flags before regions are loaded.
 */
public interface FlagRegistry {

    void register(Flag<?> flag);

    Optional<Flag<?>> getFlag(String key);

    Collection<Flag<?>> getFlags();
}
