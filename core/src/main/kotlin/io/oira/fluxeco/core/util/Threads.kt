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

    fun load() {
        val threads = maxOf(2, cfg.getString("advanced.max-pool-size")?.toIntOrNull() ?: 6)
        executor = Executors.newFixedThreadPool(threads)
        scheduledExecutor = Executors.newScheduledThreadPool(2)

        plugin.logger.info("FluxEco executor started with $threads threads.")
    }

    fun runAsync(runnable: Runnable) {
        if (shutdown || !::executor.isInitialized) return
        executor.execute(runnable)
    }

    fun runSync(runnable: Runnable) {
        if (shutdown) return
        if (Bukkit.isPrimaryThread()) {
            runnable.run()
        } else {
            plugin.foliaLib.scheduler.runNextTick { runnable.run() }
        }
    }

    fun <T> getAsync(runnable: () -> T): T? {
        if (shutdown || !::executor.isInitialized) return null
        return CompletableFuture.supplyAsync(runnable, executor).join()
    }

    fun scheduleAtFixedRate(
        initialDelay: Long,
        period: Long,
        unit: TimeUnit,
        task: Runnable
    ): ScheduledFuture<*>? {
        if (shutdown || !::scheduledExecutor.isInitialized) return null
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit)
    }

    fun schedule(delay: Long, unit: TimeUnit, task: Runnable): ScheduledFuture<*>? {
        if (shutdown || !::scheduledExecutor.isInitialized) return null
        return scheduledExecutor.schedule(task, delay, unit)
    }

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
