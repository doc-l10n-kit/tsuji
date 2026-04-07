package net.sharplab.tsuji.core.driver.translator.adaptive

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BatchProviderTest {

    @Test
    fun `GeminiBatchProvider should start with initial limit`() {
        val provider = GeminiBatchProvider(
            items = listOf("a", "b", "c"),
            initialLimit = 10,
            minLimit = 1,
            maxLimit = 20
        )

        val batch = provider.peekNext()
        assertEquals(3, batch.size)
    }

    @Test
    fun `GeminiBatchProvider should split by count`() {
        val provider = GeminiBatchProvider(
            items = listOf("a", "b", "c", "d", "e"),
            initialLimit = 2,
            minLimit = 1,
            maxLimit = 10
        )

        val batch1 = provider.peekNext()
        assertEquals(2, batch1.size)
        assertEquals(listOf("a", "b"), batch1)

        provider.consumeNext()

        val batch2 = provider.peekNext()
        assertEquals(2, batch2.size)
        assertEquals(listOf("c", "d"), batch2)
    }

    @Test
    fun `DeepLBatchProvider should split by byte size`() {
        val provider = DeepLBatchProvider(
            items = listOf("abc", "def", "ghi", "jkl"), // Each 3 bytes
            initialLimit = 10,
            minLimit = 1,
            maxLimit = 100
        )

        val batch1 = provider.peekNext()
        // 10 bytes limit: can fit 3 items (9 bytes)
        assertEquals(3, batch1.size)
        assertEquals(listOf("abc", "def", "ghi"), batch1)

        provider.consumeNext()

        val batch2 = provider.peekNext()
        assertEquals(1, batch2.size)
        assertEquals(listOf("jkl"), batch2)
    }

    @Test
    fun `should increase limit on success`() {
        val provider = GeminiBatchProvider(
            items = listOf("a", "b", "c"),
            initialLimit = 5,
            minLimit = 1,
            maxLimit = 20
        )

        val batch1 = provider.peekNext()
        assertEquals(3, batch1.size)

        provider.notifySuccess()

        // After success, next batch should use increased limit (5 -> 6)
        // But we only have 3 items, so batch size won't change
        val batch2 = provider.peekNext()
        assertEquals(3, batch2.size)
    }

    @Test
    fun `should decrease limit on validation error`() {
        val provider = GeminiBatchProvider(
            items = listOf("a", "b", "c", "d"),
            initialLimit = 10,
            minLimit = 1,
            maxLimit = 20
        )

        val batch1 = provider.peekNext()
        assertEquals(4, batch1.size)

        // Simulate validation error
        val newLimit = provider.notifyValidationError()
        assertEquals(5, newLimit) // 10 * 0.5 = 5

        val batch2 = provider.peekNext()
        // Same items, but now limited to 5
        assertEquals(4, batch2.size) // Still 4 since we only have 4 items total
    }

    @Test
    fun `should not decrease below minimum`() {
        val provider = GeminiBatchProvider(
            items = listOf("a", "b"),
            initialLimit = 2,
            minLimit = 1,
            maxLimit = 10
        )

        provider.notifyValidationError() // 2 -> 1
        assertEquals(1, provider.peekNext().size)

        provider.notifyValidationError() // Should stay at 1
        assertEquals(1, provider.peekNext().size)
    }

    @Test
    fun `should not increase above maximum`() {
        val provider = GeminiBatchProvider(
            items = List(20) { "item$it" },
            initialLimit = 10,
            minLimit = 1,
            maxLimit = 10
        )

        provider.notifySuccess() // Should stay at 10
        assertEquals(10, provider.peekNext().size)
    }
}
