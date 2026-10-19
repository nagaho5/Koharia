package koharia.komga.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KomgaSseProgressSyncQueueTest {

    @Test
    fun `book events remain bound to their originating connection`() {
        val queue = KomgaSseProgressSyncQueue()
        val serverA = KomgaSseConnectionTarget(1L, "https://a.test", generation = 1L)
        val serverB = KomgaSseConnectionTarget(2L, "https://b.test", generation = 2L)

        queue.add(PendingKomgaProgressSync(serverA, "shared-book-id"))
        queue.add(PendingKomgaProgressSync(serverA, "shared-book-id"))
        queue.add(PendingKomgaProgressSync(serverB, "shared-book-id"))

        assertEquals(
            listOf(
                PendingKomgaProgressSync(serverA, "shared-book-id"),
                PendingKomgaProgressSync(serverB, "shared-book-id"),
            ),
            queue.drain(),
        )
    }

    @Test
    fun `reconnect backoff is bounded`() {
        assertEquals(1_000L, reconnectDelayMillis(0))
        assertEquals(2_000L, reconnectDelayMillis(1))
        assertEquals(16_000L, reconnectDelayMillis(4))
        assertEquals(30_000L, reconnectDelayMillis(5))
        assertEquals(30_000L, reconnectDelayMillis(99))
    }

    @Test
    fun `clearing queue drops pending full and incremental syncs`() {
        val queue = KomgaSseProgressSyncQueue()
        val target = KomgaSseConnectionTarget(1L, "https://komga.test", generation = 1L)
        queue.add(PendingKomgaProgressSync(target, null))
        queue.add(PendingKomgaProgressSync(target, "book-1"))

        queue.clear()

        assertTrue(queue.isEmpty())
        assertTrue(queue.drain().isEmpty())
    }
}
