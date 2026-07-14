package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.plugin.bootstrap.LoaderBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The only {@link JavaPlugin} the server knows about (declared in plugin.yml).
 * Zero logic: it delegates its whole lifecycle to the bootstrap. The jar-in-jar
 * loader will slot in here later without changing the layering.
 */
public final class ZRegionsBukkitLoader extends JavaPlugin {

    private final LoaderBootstrap plugin;

    public ZRegionsBukkitLoader() {
        this.plugin = new ZRegionsBukkitBootstrap(this);
    }

    @Override
    public void onLoad() {
        this.plugin.onLoad();
    }

    @Override
    public void onEnable() {
        this.plugin.onEnable();
    }

    @Override
    public void onDisable() {
        this.plugin.onDisable();
    }
}
