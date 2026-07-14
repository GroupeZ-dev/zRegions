package fr.maxlego08.zregions.common.plugin.scheduler;

/**
 * A cancellable handle over a scheduled task.
 */
@FunctionalInterface
public interface SchedulerTask {

    void cancel();
}
