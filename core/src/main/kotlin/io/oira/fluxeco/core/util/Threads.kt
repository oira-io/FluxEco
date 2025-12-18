package io.oira.fluxeco.core.util

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.manager.ConfigManager
import org.bukkit.Bukkit
import java.util.concurrent.*

object Threads {

    private val plugin: FluxEco = FluxEco.instance
    private val cfg = ConfigManager(plugin, "config.yml").getConfig()

    lateinit var executor: ExecutorService
        private set
    lateinit var scheduledExecutor: ScheduledExecutorService
        private set
    private var shutdown = false

    /**
     * Initializes the thread subsystem by creating the fixed worker executor and the scheduled executor.
     *
     * Reads "advanced.max-pool-size" from the configuration (defaults to 6 if missing or invalid), ensures at least 2 worker threads, assigns the resulting fixed thread pool to `executor` and a 2-thread scheduled pool to `scheduledExecutor`, and logs the started thread count.
     */
    fun load() {
        val threads = maxOf(2, cfg.getString("advanced.max-pool-size")?.toIntOrNull() ?: 6)
        executor = Executors.newFixedThreadPool(threads)
        scheduledExecutor = Executors.newScheduledThreadPool(2)

        plugin.logger.info("FluxEco executor started with $threads threads.")
    }

    /**
     * Submits a task to the internal thread pool for asynchronous execution.
     *
     * If the thread subsystem is shut down or the executor is not yet initialized, the task is ignored.
     *
     * @param runnable The task to execute asynchronously.
     */
    fun runAsync(runnable: Runnable) {
        if (shutdown || !::executor.isInitialized) return
        executor.execute(runnable)
    }

    /**
     * Executes the provided Runnable on the server's primary (main) thread.
     *
     * If called from the primary thread the task runs immediately; otherwise it is scheduled to run on the next tick. If the thread subsystem has been shut down this call does nothing.
     *
     * @param runnable The task to execute on the primary Bukkit thread.
     */
    fun runSync(runnable: Runnable) {
        if (shutdown) return
        if (Bukkit.isPrimaryThread()) {
            runnable.run()
        } else {
            plugin.foliaLib.scheduler.runNextTick { runnable.run() }
        }
    }

    /**
     * Executes the given supplier on the thread pool and returns its result, or null if the thread subsystem is unavailable.
     *
     * @param runnable Supplier that produces the value to compute on the executor.
     * @return The value produced by `runnable`, or `null` if the executor is not initialized or the subsystem has been shut down.
     */
    fun <T> getAsync(runnable: () -> T): T? {
        if (shutdown || !::executor.isInitialized) return null
        return CompletableFuture.supplyAsync(runnable, executor).join()
    }

    /**
     * Schedules a repeating task to run at a fixed rate after an initial delay.
     *
     * @param initialDelay Time to wait before first execution.
     * @param period Time between successive executions.
     * @param unit Time unit for `initialDelay` and `period`.
     * @param task The runnable to execute.
     * @return The ScheduledFuture representing pending completion of the task, or `null` if the scheduler is shut down or not initialized.
     */
    fun scheduleAtFixedRate(
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledFuture<*>? {
        if (shutdown || !::scheduledExecutor.isInitialized) return null
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit)
    }

    /**
     * Schedules a one-shot task to run after the given delay.
     *
     * @param delay The delay before execution.
     * @param unit The time unit for the delay.
     * @param task The task to execute.
     * @return The ScheduledFuture representing the scheduled task, or `null` if the scheduler is not initialized or the thread subsystem is shut down.
     */
    fun schedule(delay: Long, unit: TimeUnit, task: Runnable): ScheduledFuture<*>? {
        if (shutdown || !::scheduledExecutor.isInitialized) return null
        return scheduledExecutor.schedule(task, delay, unit)
    }

    /**
     * Shuts down the thread subsystem by marking it as shut down and terminating the async and scheduled executors.
     *
     * Executors are requested to stop accepting new tasks; any exceptions raised during shutdown are caught.
     */
    fun close() {
        try {
            shutdown = true
            if (::executor.isInitialized) executor.shutdown()
            if (::scheduledExecutor.isInitialized) scheduledExecutor.shutdown()
            plugin.logger.info("Shutting down FluxEco async executor and scheduler.")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}