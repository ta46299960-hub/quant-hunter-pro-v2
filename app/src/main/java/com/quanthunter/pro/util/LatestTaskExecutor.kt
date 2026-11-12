package com.quanthunter.pro.util

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Keeps UI-triggered market work bounded and latest-request-wins.
 *
 * One worker serializes blocking provider calls. A one-entry queue and discard-oldest
 * policy prevent rapid refresh taps from building an unbounded backlog while a
 * previous network request is still waiting for its configured timeout.
 */
class LatestTaskExecutor(
    private val executor: ThreadPoolExecutor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(1),
        { runnable -> Thread(runnable, "QH-Market-Worker").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardOldestPolicy()
    )
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
