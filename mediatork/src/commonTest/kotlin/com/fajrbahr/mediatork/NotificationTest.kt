package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.notification.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationTest {

    // Strategies dispatch on the erased `suspend (Any) -> Unit` type; these direct-strategy
    // tests keep typed listeners for readability and erase them at the call site.
    @Suppress("UNCHECKED_CAST")
    private fun List<suspend (PingNotification) -> Unit>.erased(): List<suspend (Any) -> Unit> =
        this as List<suspend (Any) -> Unit>

    // ── Basic publish ──────────────────────────────────────────────────────────

    @Test
    fun `publish delivers to single handler`() = runTest {
        val received = mutableListOf<String>()
        val m = mediatorK { notification<PingNotification> { received += it.message } }
        m.publish(PingNotification("hi"))
        assertEquals(listOf("hi"), received)
    }

    @Test
    fun `publish delivers to all registered handlers`() = runTest {
        val received1 = mutableListOf<String>()
        val received2 = mutableListOf<String>()
        val m = mediatorK {
            notification<PingNotification> { received1 += it.message }
            notification<PingNotification> { received2 += it.message }
        }
        m.publish(PingNotification("hello"))
        assertEquals(listOf("hello"), received1)
        assertEquals(listOf("hello"), received2)
    }

    @Test
    fun `publish with no handlers throws by default`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("silent"))
        }
    }

    @Test
    fun `publish does not deliver to handlers of a different notification type`() = runTest {
        val pingReceived = mutableListOf<String>()
        val alertLevels = mutableListOf<Int>()
        val m = mediatorK {
            notification<PingNotification> { pingReceived += it.message }
            notification<AlertNotification> { alertLevels += it.level }
        }
        m.publish(AlertNotification(5))
        assertTrue(pingReceived.isEmpty())
        assertEquals(listOf(5), alertLevels)
    }

    @Test
    fun `publish with custom publisher overrides default for that call`() = runTest {
        val order = mutableListOf<String>()
        val m = mediatorK {
            notification<PingNotification> { order += "h1" }
            notification<PingNotification> { order += "h2" }
        }
        m.publish(PingNotification("x"), NotificationPublishStrategy.SEQUENTIAL)
        assertEquals(listOf("h1", "h2"), order)
    }

    // ── SequentialNotificationPublisher ───────────────────────────────────────

    @Test
    fun `SequentialNotificationPublisher invokes handlers in order`() = runTest {
        val order = mutableListOf<Int>()
        val listeners = (1..3).map { i ->
            suspend { _: PingNotification -> order += i }
        }
        val pub = NotificationPublishStrategy.SEQUENTIAL
        pub.publish(PingNotification("x"), listeners.erased())
        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun `SequentialNotificationPublisher stops on first exception`() = runTest {
        val ran = mutableListOf<Int>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { ran += 1 },
            { throw RuntimeException("fail") },
            { ran += 3 },
        )
        val pub = NotificationPublishStrategy.SEQUENTIAL
        assertFailsWith<RuntimeException> { pub.publish(PingNotification("x"), listeners.erased()) }
        assertEquals(listOf(1), ran)
    }

    // ── ParallelNotificationPublisher ─────────────────────────────────────────

    @Test
    fun `ParallelNotificationPublisher delivers to all handlers`() = runTest {
        val received1 = mutableListOf<String>()
        val received2 = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { received1 += it.message },
            { received2 += it.message },
        )
        val pub = NotificationPublishStrategy.PARALLEL
        pub.publish(PingNotification("par"), listeners.erased())
        assertEquals(listOf("par"), received1)
        assertEquals(listOf("par"), received2)
    }

    @Test
    fun `ParallelNotificationPublisher propagates exception`() = runTest {
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { throw IllegalStateException("boom") },
        )
        val pub = NotificationPublishStrategy.PARALLEL
        assertFailsWith<IllegalStateException> { pub.publish(PingNotification("x"), listeners.erased()) }
    }

    @Test
    fun `ParallelNotificationPublisher with no handlers returns without error`() = runTest {
        val pub = NotificationPublishStrategy.PARALLEL
        pub.publish(PingNotification("x"), emptyList())
    }

    // ── ContinueOnExceptionNotificationPublisher ──────────────────────────────

    @Test
    fun `ContinueOnException runs all handlers even when one throws`() = runTest {
        val ran = mutableListOf<Int>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { ran += 1 },
            { throw RuntimeException("fail") },
            { ran += 3 },
        )
        val pub = NotificationPublishStrategy.CONTINUE_ON_EXCEPTION
        assertFailsWith<AggregateException> { pub.publish(PingNotification("x"), listeners.erased()) }
        assertEquals(listOf(1, 3), ran)
    }

    @Test
    fun `ContinueOnException collects all failures into AggregateException`() = runTest {
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { throw RuntimeException("e1") },
            { throw RuntimeException("e2") },
        )
        val pub = NotificationPublishStrategy.CONTINUE_ON_EXCEPTION
        val ex = assertFailsWith<AggregateException> { pub.publish(PingNotification("x"), listeners.erased()) }
        assertTrue(ex.message!!.contains("2"))
    }

    @Test
    fun `ContinueOnException does not throw when all handlers succeed`() = runTest {
        val received = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { received += it.message },
        )
        val pub = NotificationPublishStrategy.CONTINUE_ON_EXCEPTION
        pub.publish(PingNotification("ok"), listeners.erased())
        assertEquals(listOf("ok"), received)
    }

    @Test
    fun `ContinueOnException does not throw with empty handler list`() = runTest {
        val pub = NotificationPublishStrategy.CONTINUE_ON_EXCEPTION
        pub.publish(PingNotification("x"), emptyList())
    }

    // ── FireAndForgetNotificationPublisher ────────────────────────────────────

    @Test
    fun `FireAndForget returns immediately and still delivers`() = runTest {
        val received = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { received += it.message },
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val pub = NotificationPublishStrategy.fireAndForget(scope)
        pub.publish(PingNotification("ff"), listeners.erased())
        assertEquals(listOf("ff"), received)
    }

    @Test
    fun `FireAndForget launches handlers on provided scope and returns`() = runTest {
        val ran = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { ran += it.message },
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val pub = NotificationPublishStrategy.fireAndForget(scope)
        pub.publish(PingNotification("ff2"), listeners.erased())
        assertEquals(listOf("ff2"), ran)
    }

    // ── ThrowMissingNotificationHandler (default behavior) ───────────────────

    @Test
    fun `default mediator throws when no notification handlers registered`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("x"))
        }
    }

    @Test
    fun `MissingNotificationHandlerException contains notification type name`() = runTest {
        val m = mediatorK { }
        val ex = assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("x"))
        }
        assertTrue(ex.message!!.contains("PingNotification"))
    }

    @Test
    fun `default mediator does not throw when handlers are registered`() = runTest {
        val received = mutableListOf<String>()
        val m = mediatorK { notification<PingNotification> { received += it.message } }
        m.publish(PingNotification("ok"))
        assertEquals(listOf("ok"), received)
    }

    // ── SilentMissingNotification (onMissingNotification) ────────────────────

    @Test
    fun `silent onMissingNotification does not throw when no handlers registered`() = runTest {
        val m = mediatorK { onMissingNotification = { } }
        m.publish(PingNotification("dropped"))
    }

    @Test
    fun `silent onMissingNotification still delivers when handlers are registered`() = runTest {
        val received = mutableListOf<String>()
        val m = mediatorK {
            onMissingNotification = { }
            notification<PingNotification> { received += it.message }
        }
        m.publish(PingNotification("ok"))
        assertEquals(listOf("ok"), received)
    }

    // ── NotificationPublishStrategy companion constants ────────────────────────

    @Test
    fun `DEFAULT companion constant delivers to all handlers`() = runTest {
        val received1 = mutableListOf<String>()
        val received2 = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { received1 += it.message },
            { received2 += it.message },
        )
        val pub = NotificationPublishStrategy.DEFAULT
        pub.publish(PingNotification("def"), listeners.erased())
        assertEquals(listOf("def"), received1)
        assertEquals(listOf("def"), received2)
    }

    @Test
    fun `PARALLEL companion constant delivers to all handlers`() = runTest {
        val received = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { received += it.message },
        )
        val pub = NotificationPublishStrategy.PARALLEL
        pub.publish(PingNotification("par"), listeners.erased())
        assertEquals(listOf("par"), received)
    }

    @Test
    fun `SEQUENTIAL companion constant delivers in registration order`() = runTest {
        val order = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { order += "h1" },
            { order += "h2" },
        )
        NotificationPublishStrategy.SEQUENTIAL.publish(PingNotification("x"), listeners.erased())
        assertEquals(listOf("h1", "h2"), order)
    }

    @Test
    fun `CONTINUE_ON_EXCEPTION companion constant collects all failures`() = runTest {
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { throw RuntimeException("e1") },
            { throw RuntimeException("e2") },
        )
        val ex = assertFailsWith<AggregateException> {
            NotificationPublishStrategy.CONTINUE_ON_EXCEPTION.publish(PingNotification("x"), listeners.erased())
        }
        assertTrue(ex.message!!.contains("2"))
    }

    @Test
    fun `fireAndForget companion function returns FireAndForgetPublisher`() = runTest {
        val received = mutableListOf<String>()
        val listeners: List<suspend (PingNotification) -> Unit> = listOf(
            { received += it.message },
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val pub = NotificationPublishStrategy.fireAndForget(scope)
        pub.publish(PingNotification("ff"), listeners.erased())
        assertEquals(listOf("ff"), received)
    }
}
