package fr.maxlego08.zregions.common.config;

import java.util.Collection;
import java.util.List;

/**
 * Platform-agnostic view over a YAML file. Implemented per platform
 * (Bukkit: YamlConfiguration) so the common module never parses YAML itself.
 */
public interface ConfigurationAdapter {

    String getString(String path, String def);

    int getInt(String path, int def);

    boolean getBoolean(String path, boolean def);

    List<String> getStringList(String path, List<String> def);

    /** The direct child keys of a section (empty when absent). */
    Collection<String> getKeys(String path);

    void reload();
}
