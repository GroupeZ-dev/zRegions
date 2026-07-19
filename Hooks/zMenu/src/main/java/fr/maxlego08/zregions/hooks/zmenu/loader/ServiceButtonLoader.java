package fr.maxlego08.zregions.hooks.zmenu.loader;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

/**
 * Generic loader for zRegions buttons: they read no extra YAML keys, they only
 * need the {@code ZMenuGuiService}, so one factory-backed loader covers every
 * type (a fresh button per inventory load, as zMenu expects).
 */
public final class ServiceButtonLoader extends ButtonLoader {

    private final Supplier<Button> factory;

    public ServiceButtonLoader(Plugin plugin, String type, Supplier<Button> factory) {
        super(plugin, type);
        this.factory = factory;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        return this.factory.get();
    }
}
