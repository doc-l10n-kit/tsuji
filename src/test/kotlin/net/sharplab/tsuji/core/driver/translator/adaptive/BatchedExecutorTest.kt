package net.sharplab.tsuji.core.driver.translator.adaptive

import kotlinx.coroutines.runBlocking
import net.sharplab.tsuji.core.driver.translator.exception.ResponseParseException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BatchedExecutorTest {

    @Test
    fun `should return empty list for empty items`(): Unit = runBlocking {
        // Given
        val batchProvider = createBatchProvider(items = emptyList(), initialLimit = 10)
        val executor = BatchedExecutor(batchProvider = batchProvider)

        // When
        val result = executor.execute { batch -> batch.map { it.uppercase() } }

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `should process single batch`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c")
        val batchProvider = createBatchProvider(items = items, initialLimit = 10)
        val executor = BatchedExecutor(batchProvider = batchProvider)

        // When
        val result = executor.execute { batch -> batch.map { it.uppercase() } }

        // Then
        assertThat(result).containsExactly("A", "B", "C")
    }

    @Test
    fun `should split into multiple batches based on size limit`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c", "d", "e")
        val batchProvider = createBatchProvider(items = items, initialLimit = 2, maxLimit = 2)
        val executor = BatchedExecutor(batchProvider = batchProvider)

        val batches = mutableListOf<List<String>>()

        // When
        val result = executor.execute { batch ->
            batches.add(batch)
            batch.map { it.uppercase() }
        }

        // Then
        assertThat(result).containsExactly("A", "B", "C", "D", "E")
        assertThat(batches).hasSize(3)
        assertThat(batches[0]).containsExactly("a", "b")
        assertThat(batches[1]).containsExactly("c", "d")
        assertThat(batches[2]).containsExactly("e")
    }

    @Test
    fun `should call onBatchSuccess on successful processing`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c")
        val batchProvider = CountBasedBatchProvider(
            items = items,
            initialLimit = 10,
            minLimit = 1,
            maxLimit = 20
        )
        val executor = BatchedExecutor(batchProvider = batchProvider)

        // When
        executor.execute { batch -> batch.map { it.uppercase() } }

        // Then: verify provider state (all items consumed)
        assertThat(batchProvider.hasNext()).isFalse()
    }

    @Test
    fun `should reduce batch size and recreate batch on validation error`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c", "d")
        val batchProvider = createBatchProvider(items = items, initialLimit = 4, maxLimit = 4)
        val executor = BatchedExecutor(
            batchProvider = batchProvider,
            maxValidationRetries = 5
        )

        var attemptCount = 0
        val batches = mutableListOf<List<String>>()

        // When
        val result = executor.execute<String> { batch ->
            batches.add(batch)
            attemptCount++

            // First attempt: batch size 4 -> validation error
            if (attemptCount == 1) {
                throw ResponseParseException("Batch too large")
            }

            // Second attempt should have smaller batch (size 2 after halving)
            batch.map { it.uppercase() }
        }

        // Then
        assertThat(result).containsExactly("A", "B", "C", "D")
        assertThat(batches).hasSize(3)
        assertThat(batches[0]).hasSize(4) // First attempt (failed)
        assertThat(batches[1]).hasSize(2) // Second attempt (success) - halved
        assertThat(batches[2]).hasSize(2) // Third batch
    }

    @Test
    fun `should retry on general exception`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c")
        val batchProvider = createBatchProvider(items = items, initialLimit = 3)
        val executor = BatchedExecutor(
            batchProvider = batchProvider,
            maxRetries = 3
        )

        var attemptCount = 0

        // When
        val result = executor.execute { batch ->
            attemptCount++

            // First two attempts fail with general exception
            if (attemptCount <= 2) {
                throw RuntimeException("Network error")
            }

            // Third attempt succeeds
            batch.map { it.uppercase() }
        }

        // Then
        assertThat(result).containsExactly("A", "B", "C")
        assertThat(attemptCount).isEqualTo(3)
    }

    @Test
    fun `should throw exception after max validation retries`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c")
        val batchProvider = createBatchProvider(items = items, initialLimit = 10, minLimit = 1, maxLimit = 10)
        val executor = BatchedExecutor(
            batchProvider = batchProvider,
            maxValidationRetries = 3
        )

        // When/Then
        val exception = assertThrows<ResponseParseException> {
            executor.execute<String> { batch ->
                throw ResponseParseException("Always fails")
            }
        }

        assertThat(exception.message).isEqualTo("Always fails")
    }

    @Test
    fun `should throw exception after max general retries`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c")
        val batchProvider = createBatchProvider(items = items, initialLimit = 3)
        val executor = BatchedExecutor(
            batchProvider = batchProvider,
            maxRetries = 2
        )

        // When/Then
        val exception = assertThrows<RuntimeException> {
            executor.execute<String> { batch ->
                throw RuntimeException("Network error")
            }
        }

        assertThat(exception.message).isEqualTo("Network error")
    }

    @Test
    fun `should handle byte size based batching`(): Unit = runBlocking {
        // Given: max 10 bytes per batch
        val items = listOf("abc", "def", "ghi", "jkl") // Each 3 bytes
        val batchProvider = DeepLBatchProvider(
            items = items,
            initialLimit = 10,
            minLimit = 1,
            maxLimit = 100
        )
        val executor = BatchedExecutor(batchProvider = batchProvider)

        val batches = mutableListOf<List<String>>()

        // When
        val result = executor.execute { batch ->
            batches.add(batch)
            batch.map { it.uppercase() }
        }

        // Then
        assertThat(result).containsExactly("ABC", "DEF", "GHI", "JKL")
        // 10 bytes limit: can fit 3 items (9 bytes), then 1 item (3 bytes)
        assertThat(batches).hasSize(2)
        assertThat(batches[0]).containsExactly("abc", "def", "ghi") // 9 bytes
        assertThat(batches[1]).containsExactly("jkl") // 3 bytes
    }

    @Test
    fun `should process batches sequentially and maintain position correctly`(): Unit = runBlocking {
        // Given
        val items = listOf("a", "b", "c", "d")
        val batchProvider = createBatchProvider(items = items, initialLimit = 2, maxLimit = 2)
        val executor = BatchedExecutor(batchProvider = batchProvider)

        val processedBatches = mutableListOf<List<String>>()

        // When
        val result = executor.execute { batch ->
            processedBatches.add(batch)
            batch.map { it.uppercase() }
        }

        // Then
        assertThat(result).containsExactly("A", "B", "C", "D")
        assertThat(processedBatches).hasSize(2)
        assertThat(processedBatches[0]).containsExactly("a", "b")
        assertThat(processedBatches[1]).containsExactly("c", "d")
    }

    private fun createBatchProvider(
        items: List<String>,
        initialLimit: Int,
        minLimit: Int = 1,
        maxLimit: Int = 100
    ): BatchProvider<String> {
        return CountBasedBatchProvider(
            items = items,
            initialLimit = initialLimit,
            minLimit = minLimit,
            maxLimit = maxLimit
        )
    }
}
