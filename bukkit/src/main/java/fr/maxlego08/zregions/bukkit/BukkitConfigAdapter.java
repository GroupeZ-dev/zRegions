package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * {@link ConfigurationAdapter} over a Bukkit {@link YamlConfiguration} file.
 */
public final class BukkitConfigAdapter implements ConfigurationAdapter {

    private final Path file;
    private YamlConfiguration configuration;

    public BukkitConfigAdapter(Path file) {
        this.file = file;
        reload();
    }

    @Override
    public String getString(String path, String def) {
        return this.configuration.getString(path, def);
    }

    @Override
    public int getInt(String path, int def) {
        return this.configuration.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return this.configuration.getBoolean(path, def);
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        return this.configuration.isList(path) ? this.configuration.getStringList(path) : def;
    }

    @Override
    public Collection<String> getKeys(String path) {
        ConfigurationSection section = this.configuration.getConfigurationSection(path);
        return section == null ? List.of() : section.getKeys(false);
    }

    @Override
    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(this.file.toFile());
    }
}
