package com.quanthunter.pro.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Keeps UI-triggered market work bounded and latest-request-wins.
 * A single worker prevents rapid navigation or refresh taps from spawning unbounded threads.
 */
class LatestTaskExecutor(
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "QH-Market-Worker").apply { isDaemon = true }
    }
) : AutoCloseable {
    private var activeTask: Future<*>? = null

    @Synchronized
    fun submit(task: () -> Unit) {
        activeTask?.cancel(true)
        activeTask = executor.submit { task() }
    }

    @Synchronized
    fun cancel() {
        activeTask?.cancel(true)
        activeTask = null
    }

    @Synchronized
    override fun close() {
        cancel()
        executor.shutdownNow()
    }
}
