package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.FlagRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link FlagRegistry}: a concurrent map keyed by lowercase flag key,
 * safe for addon registration during startup.
 */
public final class ZFlagRegistry implements FlagRegistry {

    private final Map<String, Flag<?>> flags = new ConcurrentHashMap<>();

    @Override
    public void register(Flag<?> flag) {
        this.flags.put(flag.getKey().toLowerCase(Locale.ROOT), flag);
    }

    @Override
    public Optional<Flag<?>> getFlag(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(this.flags.get(key.toLowerCase(Locale.ROOT)));
    }

    @Override
    public Collection<Flag<?>> getFlags() {
        return Collections.unmodifiableCollection(this.flags.values());
    }
}
