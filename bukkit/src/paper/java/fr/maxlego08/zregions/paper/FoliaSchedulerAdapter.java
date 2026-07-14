package fr.maxlego08.zregions.paper;

import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import fr.maxlego08.zregions.common.plugin.scheduler.JavaSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Scheduler for Folia servers, routing sync work to the global region scheduler.
 * This class lives in the paper sourceSet and is only instantiated by reflection
 * after a runtime Folia check — pure Spigot never loads it.
 */
public class FoliaSchedulerAdapter extends JavaSchedulerAdapter {

    private final Plugin plugin;

    public FoliaSchedulerAdapter(Plugin plugin, PluginLogger logger) {
        super(logger);
        this.plugin = plugin;
    }

    @Override
    public void executeSync(Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(this.plugin, task);
    }
}
