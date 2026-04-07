package net.sharplab.tsuji.core.driver.translator.adaptive

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger

class AdaptiveParallelismControllerTest {

    // Test exception type
    class TestRateLimitException(message: String) : Exception(message)

    @Test
    fun `should enforce initial concurrency limit`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 2,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        val concurrentExecutions = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)

        // Launch 5 concurrent requests
        val jobs = List(5) {
            async {
                controller.execute {
                    val current = concurrentExecutions.incrementAndGet()
                    maxObservedConcurrency.updateAndGet { max -> maxOf(max, current) }
                    delay(50) // Simulate work
                    concurrentExecutions.decrementAndGet()
                }
            }
        }

        jobs.forEach { it.await() }

        // Should never exceed initial concurrency of 2
        assertEquals(2, maxObservedConcurrency.get())
    }

    @Test
    fun `should increase concurrency after consecutive successes`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 2,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(2, controller.getCurrentConcurrency())

        // Execute 10 successful requests (threshold for increase)
        repeat(10) {
            controller.execute {
                delay(1)
            }
        }

        // Should increase by 1
        assertEquals(3, controller.getCurrentConcurrency())

        // Another 10 successes
        repeat(10) {
            controller.execute {
                delay(1)
            }
        }

        // Should increase by 1 again
        assertEquals(4, controller.getCurrentConcurrency())
    }

    @Test
    fun `should decrease concurrency on rate limit error`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(3, controller.getCurrentConcurrency())

        // Trigger rate limit error
        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit exceeded")
            }
        }

        // Should decrease by 1
        assertEquals(2, controller.getCurrentConcurrency())

        // Another rate limit error
        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit exceeded")
            }
        }

        // Should decrease by 1 again
        assertEquals(1, controller.getCurrentConcurrency())
    }

    @Test
    fun `should not decrease concurrency below minimum`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 1,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(1, controller.getCurrentConcurrency())

        // Trigger rate limit error
        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit exceeded")
            }
        }

        // Should stay at minimum
        assertEquals(1, controller.getCurrentConcurrency())
    }

    @Test
    fun `should not increase concurrency above maximum`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 3,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(3, controller.getCurrentConcurrency())

        // Execute 10 successful requests
        repeat(10) {
            controller.execute {
                delay(1)
            }
        }

        // Should stay at maximum
        assertEquals(3, controller.getCurrentConcurrency())
    }

    @Test
    fun `should reset success counter on rate limit error`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 2,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        // 9 successful requests (not enough to increase)
        repeat(9) {
            controller.execute {
                delay(1)
            }
        }

        assertEquals(2, controller.getCurrentConcurrency())

        // Rate limit error resets counter
        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit exceeded")
            }
        }

        assertEquals(1, controller.getCurrentConcurrency())

        // Need 10 more successes to increase
        repeat(10) {
            controller.execute {
                delay(1)
            }
        }

        assertEquals(2, controller.getCurrentConcurrency())
    }

    @Test
    fun `should re-throw non-rate-limit exceptions without decreasing concurrency`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(3, controller.getCurrentConcurrency())

        // Trigger different exception
        assertThrows<IllegalStateException> {
            controller.execute {
                throw IllegalStateException("Some other error")
            }
        }

        // Should not decrease concurrency
        assertEquals(3, controller.getCurrentConcurrency())
    }

    @Test
    fun `should handle concurrent requests from multiple coroutines`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 10,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        val completedCount = AtomicInteger(0)

        // Launch 20 concurrent requests
        val jobs = List(20) {
            async {
                controller.execute {
                    delay(10)
                    completedCount.incrementAndGet()
                }
            }
        }

        jobs.forEach { it.await() }

        // All requests should complete
        assertEquals(20, completedCount.get())
    }

    @Test
    fun `should keep permit as reserved when rate limit error occurs`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(3, controller.getCurrentConcurrency())

        // Trigger rate limit error
        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit exceeded")
            }
        }

        assertEquals(2, controller.getCurrentConcurrency())

        // Verify that actual concurrency is limited to 2
        val concurrentExecutions = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)

        val jobs = List(5) {
            async {
                controller.execute {
                    val current = concurrentExecutions.incrementAndGet()
                    maxObservedConcurrency.updateAndGet { max -> maxOf(max, current) }
                    delay(50)
                    concurrentExecutions.decrementAndGet()
                }
            }
        }

        jobs.forEach { it.await() }

        // Should never exceed current limit of 2
        assertEquals(2, maxObservedConcurrency.get())
    }

    @Test
    fun `should release permit when rate limit error occurs at minimum concurrency`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 1,
            minConcurrency = 1,
            maxConcurrency = 5,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(1, controller.getCurrentConcurrency())

        // Trigger rate limit error at minimum
        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit exceeded")
            }
        }

        // Should stay at minimum
        assertEquals(1, controller.getCurrentConcurrency())

        // Verify that permit was released (not kept as reserved)
        // If permit was kept, this request would block forever
        var executed = false
        controller.execute {
            executed = true
        }

        assertTrue(executed, "Request should have executed (permit should have been released)")
    }

    @Test
    fun `should handle multiple concurrent rate limit errors without deadlock`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 5,
            minConcurrency = 1,
            maxConcurrency = 10,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(5, controller.getCurrentConcurrency())

        // Trigger multiple rate limit errors concurrently
        val jobs = List(3) {
            async {
                assertThrows<TestRateLimitException> {
                    controller.execute {
                        delay(10) // Simulate some work before error
                        throw TestRateLimitException("Rate limit exceeded")
                    }
                }
            }
        }

        jobs.forEach { it.await() }

        // All errors should complete without deadlock
        // Concurrency should have decreased
        assertTrue(controller.getCurrentConcurrency() < 5)
        assertTrue(controller.getCurrentConcurrency() >= 1)
    }

    @Test
    fun `should maintain correct concurrency after mixed success and error scenarios`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 10,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(3, controller.getCurrentConcurrency())

        // Pattern: success, success, error, success, success
        controller.execute { delay(1) }
        controller.execute { delay(1) }

        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit")
            }
        }

        assertEquals(2, controller.getCurrentConcurrency())

        // Verify actual concurrency matches currentLimit
        val concurrentExecutions = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)

        val jobs = List(4) {
            async {
                controller.execute {
                    val current = concurrentExecutions.incrementAndGet()
                    maxObservedConcurrency.updateAndGet { max -> maxOf(max, current) }
                    delay(50)
                    concurrentExecutions.decrementAndGet()
                }
            }
        }

        jobs.forEach { it.await() }

        assertEquals(2, maxObservedConcurrency.get())
    }

    @Test
    fun `should correctly adjust reserved permits through increase and decrease cycles`() = runBlocking {
        val controller = AdaptiveParallelismController(
            initialConcurrency = 2,
            minConcurrency = 1,
            maxConcurrency = 4,
            rateLimitExceptionClass = TestRateLimitException::class
        )

        assertEquals(2, controller.getCurrentConcurrency())

        // Increase to 3 (10 successes needed)
        repeat(10) {
            controller.execute { delay(1) }
        }
        assertEquals(3, controller.getCurrentConcurrency())

        // Decrease to 2 (rate limit error)
        assertThrows<TestRateLimitException> {
            controller.execute {
                throw TestRateLimitException("Rate limit")
            }
        }
        assertEquals(2, controller.getCurrentConcurrency())

        // Verify actual concurrency after adjustment
        val concurrentExecutions = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)

        val jobs = List(5) {
            async {
                controller.execute {
                    val current = concurrentExecutions.incrementAndGet()
                    maxObservedConcurrency.updateAndGet { max -> maxOf(max, current) }
                    delay(30)
                    concurrentExecutions.decrementAndGet()
                }
            }
        }

        jobs.forEach { it.await() }

        // Should match currentLimit
        assertEquals(2, maxObservedConcurrency.get())
    }
}
