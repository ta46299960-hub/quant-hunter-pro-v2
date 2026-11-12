package com.quanthunter.pro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LatestTaskExecutorTest {
    @Test
    fun replacementInterruptsActiveTaskAndRunsLatestTaskOnSingleWorker() {
        val runner = LatestTaskExecutor()
        val firstStarted = CountDownLatch(1)
        val firstInterrupted = CountDownLatch(1)
        val latestFinished = CountDownLatch(1)
        val activeTasks = AtomicInteger()
        val maxConcurrentTasks = AtomicInteger()

        try {
            runner.submit {
                val active = activeTasks.incrementAndGet()
                maxConcurrentTasks.updateAndGet { maxOf(it, active) }
                firstStarted.countDown()
                try {
                    CountDownLatch(1).await()
                } catch (_: InterruptedException) {
                    firstInterrupted.countDown()
                } finally {
                    activeTasks.decrementAndGet()
                }
            }

            assertTrue("First market task should start", firstStarted.await(2, TimeUnit.SECONDS))

            runner.submit {
                val active = activeTasks.incrementAndGet()
                maxConcurrentTasks.updateAndGet { maxOf(it, active) }
                activeTasks.decrementAndGet()
                latestFinished.countDown()
            }

            assertTrue("Replaced task should be interrupted", firstInterrupted.await(2, TimeUnit.SECONDS))
            assertTrue("Latest task should execute", latestFinished.await(2, TimeUnit.SECONDS))
            assertEquals(1, maxConcurrentTasks.get())
        } finally {
            runner.close()
        }
    }
}
