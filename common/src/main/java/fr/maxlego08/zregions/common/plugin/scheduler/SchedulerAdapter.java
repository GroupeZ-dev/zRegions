package fr.maxlego08.zregions.common.plugin.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Platform scheduler abstraction (LuckPerms model): the async side is implemented
 * once in {@link JavaSchedulerAdapter} with plain JDK executors; platforms only
 * provide the "sync" (game thread) execution.
 */
public interface SchedulerAdapter {

    /** The async worker pool. Storage writes, imports and housekeeping run here. */
    Executor async();

    default void executeAsync(Runnable task) {
        async().execute(task);
    }

    /** Runs a task on the game thread (region thread on Folia). */
    void executeSync(Runnable task);

    SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit);

    SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit);

    /** Stops the delayed/repeating scheduler. */
    void shutdownScheduler();

    /** Stops the async worker pool. */
    void shutdownExecutor();
}
