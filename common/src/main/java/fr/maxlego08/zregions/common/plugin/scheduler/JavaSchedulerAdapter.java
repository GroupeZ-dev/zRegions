package fr.maxlego08.zregions.common.plugin.scheduler;

import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The async half of the {@link SchedulerAdapter}, implemented once with plain JDK
 * executors (ported from LuckPerms' JavaSchedulerAdapter, MIT). Platforms extend
 * this and only add {@link #executeSync(Runnable)}.
 */
public abstract class JavaSchedulerAdapter implements SchedulerAdapter {

    private final ScheduledThreadPoolExecutor scheduler;
    private final ForkJoinPool worker;
    private final PluginLogger logger;

    protected JavaSchedulerAdapter(PluginLogger logger) {
        this.logger = logger;
        this.scheduler = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "zregions-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler.setRemoveOnCancelPolicy(true);
        this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.worker = new ForkJoinPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
    }

    @Override
    public Executor async() {
        return this.worker;
    }

    @Override
    public SchedulerTask asyncLater(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> future = this.scheduler.schedule(() -> this.worker.execute(task), delay, unit);
        return () -> future.cancel(false);
    }

    @Override
    public SchedulerTask asyncRepeating(Runnable task, long interval, TimeUnit unit) {
        ScheduledFuture<?> future = this.scheduler.scheduleAtFixedRate(() -> this.worker.execute(task), interval, interval, unit);
        return () -> future.cancel(false);
    }

    @Override
    public void shutdownScheduler() {
        this.scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                this.logger.severe("Timed out waiting for the zRegions scheduler to terminate");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void shutdownExecutor() {
        this.worker.shutdown();
        try {
            if (!this.worker.awaitTermination(30, TimeUnit.SECONDS)) {
                this.logger.severe("Timed out waiting for the zRegions worker pool to terminate");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
