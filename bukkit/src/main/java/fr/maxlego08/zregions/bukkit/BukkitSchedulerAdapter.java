package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import fr.maxlego08.zregions.common.plugin.scheduler.JavaSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Scheduler for regular Bukkit/Spigot servers: the async half comes from
 * {@link JavaSchedulerAdapter}, sync work goes through the Bukkit scheduler.
 */
public class BukkitSchedulerAdapter extends JavaSchedulerAdapter {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin, PluginLogger logger) {
        super(logger);
        this.plugin = plugin;
    }

    @Override
    public void executeSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, task);
        }
    }
}
