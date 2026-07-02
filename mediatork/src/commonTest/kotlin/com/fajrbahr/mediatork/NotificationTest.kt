@file:Suppress("TooGenericExceptionThrown")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.notification.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationTest {

    // ── Basic publish ──────────────────────────────────────────────────────────

    @Test
    fun `publish delivers to single handler`() = runTest {
        val h = RecordingNotificationHandler()
        val m = mediator { registerNotification(h) }
        m.publish(PingNotification("hi"))
        assertEquals(listOf("hi"), h.received)
    }

    @Test
    fun `publish delivers to all registered handlers`() = runTest {
        val h1 = RecordingNotificationHandler()
        val h2 = RecordingNotificationHandler()
        val m = mediator {
            registerNotification(h1)
            registerNotification(h2)
        }
        m.publish(PingNotification("hello"))
        assertEquals(listOf("hello"), h1.received)
        assertEquals(listOf("hello"), h2.received)
    }

    @Test
    fun `publish with no handlers throws by default`() = runTest {
        val m = mediator { }
        assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("silent"))
        }
    }

    @Test
    fun `publish does not deliver to handlers of a different notification type`() = runTest {
        val pingHandler = RecordingNotificationHandler()
        val alertHandler = AlertNotificationHandler()
        val m = mediator {
            registerNotification(pingHandler)
            registerNotification(alertHandler)
        }
        m.publish(AlertNotification(5))
        assertTrue(pingHandler.received.isEmpty())
        assertEquals(listOf(5), alertHandler.levels)
    }

    @Test
    fun `publish with custom publisher overrides default for that call`() = runTest {
        val order = mutableListOf<String>()
        val h1 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                order += "h1"
            }
        }
        val h2 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                order += "h2"
            }
        }
        val m = mediator {
            registerNotification(h1)
            registerNotification(h2)
        }
        m.publish(PingNotification("x"), SequentialNotificationPublisher())
        assertEquals(listOf("h1", "h2"), order)
    }

    // ── SequentialNotificationPublisher ───────────────────────────────────────

    @Test
    fun `SequentialNotificationPublisher invokes handlers in order`() = runTest {
        val order = mutableListOf<Int>()
        val handlers = (1..3).map { i ->
            object : NotificationHandler<PingNotification> {
                override suspend fun handle(notification: PingNotification) {
                    order += i
                }
            }
        }
        val pub = SequentialNotificationPublisher()
        pub.publish(PingNotification("x"), handlers)
        assertEquals(listOf(1, 2, 3), order)
    }

    @Test
    fun `SequentialNotificationPublisher stops on first exception`() = runTest {
        val ran = mutableListOf<Int>()
        val h1 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                ran += 1
            }
        }
        val h2 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                throw RuntimeException("fail"); ran += 2
            }
        }
        val h3 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                ran += 3
            }
        }
        val pub = SequentialNotificationPublisher()
        assertFailsWith<RuntimeException> { pub.publish(PingNotification("x"), listOf(h1, h2, h3)) }
        assertEquals(listOf(1), ran)
    }

    // ── ParallelNotificationPublisher ─────────────────────────────────────────

    @Test
    fun `ParallelNotificationPublisher delivers to all handlers`() = runTest {
        val h1 = RecordingNotificationHandler()
        val h2 = RecordingNotificationHandler()
        val pub = ParallelNotificationPublisher()
        pub.publish(PingNotification("par"), listOf(h1, h2))
        assertEquals(listOf("par"), h1.received)
        assertEquals(listOf("par"), h2.received)
    }

    @Test
    fun `ParallelNotificationPublisher propagates exception`() = runTest {
        val failing = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                error("boom")
            }
        }
        val pub = ParallelNotificationPublisher()
        assertFailsWith<IllegalStateException> { pub.publish(PingNotification("x"), listOf(failing)) }
    }

    @Test
    fun `ParallelNotificationPublisher with no handlers returns without error`() = runTest {
        val pub = ParallelNotificationPublisher()
        pub.publish(PingNotification("x"), emptyList())
    }

    // ── ContinueOnExceptionNotificationPublisher ──────────────────────────────

    @Test
    fun `ContinueOnException runs all handlers even when one throws`() = runTest {
        val ran = mutableListOf<Int>()
        val h1 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                ran += 1
            }
        }
        val h2 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                throw RuntimeException("fail"); ran += 2
            }
        }
        val h3 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                ran += 3
            }
        }
        val pub = ContinueOnExceptionNotificationPublisher()
        assertFailsWith<AggregateException> { pub.publish(PingNotification("x"), listOf(h1, h2, h3)) }
        assertEquals(listOf(1, 3), ran)
    }

    @Test
    fun `ContinueOnException collects all failures into AggregateException`() = runTest {
        val h1 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                throw RuntimeException("e1")
            }
        }
        val h2 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                throw RuntimeException("e2")
            }
        }
        val pub = ContinueOnExceptionNotificationPublisher()
        val ex = assertFailsWith<AggregateException> { pub.publish(PingNotification("x"), listOf(h1, h2)) }
        assertTrue(ex.message!!.contains("2"))
    }

    @Test
    fun `ContinueOnException does not throw when all handlers succeed`() = runTest {
        val h = RecordingNotificationHandler()
        val pub = ContinueOnExceptionNotificationPublisher()
        pub.publish(PingNotification("ok"), listOf(h))
        assertEquals(listOf("ok"), h.received)
    }

    @Test
    fun `ContinueOnException does not throw with empty handler list`() = runTest {
        val pub = ContinueOnExceptionNotificationPublisher()
        pub.publish(PingNotification("x"), emptyList())
    }

    // ── FireAndForgetNotificationPublisher ────────────────────────────────────

    @Test
    fun `FireAndForget returns immediately and still delivers`() = runTest {
        val h = RecordingNotificationHandler()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val pub = FireAndForgetNotificationPublisher(scope)
        pub.publish(PingNotification("ff"), listOf(h))
        assertEquals(listOf("ff"), h.received)
    }

    @Test
    fun `FireAndForget launches handlers on provided scope and returns`() = runTest {
        val ran = mutableListOf<String>()
        val h = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                ran += notification.message
            }
        }
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val pub = FireAndForgetNotificationPublisher(scope)
        pub.publish(PingNotification("ff2"), listOf(h))
        assertEquals(listOf("ff2"), ran)
    }

    // ── ThrowMissingNotificationHandlerStrategy ───────────────────────────────

    @Test
    fun `ThrowMissingNotificationHandlerStrategy throws when no handlers registered`() = runTest {
        val m = mediator(
            missingNotificationHandler = ThrowMissingNotificationHandler()
        ) { }
        assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("x"))
        }
    }

    @Test
    fun `ThrowMissingNotificationHandlerStrategy exception contains notification type name`() = runTest {
        val m = mediator(
            missingNotificationHandler = ThrowMissingNotificationHandler()
        ) { }
        val ex = assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("x"))
        }
        assertTrue(ex.message!!.contains("PingNotification"))
    }

    @Test
    fun `ThrowMissingNotificationHandlerStrategy does not throw when handlers are registered`() = runTest {
        val h = RecordingNotificationHandler()
        val m = mediator(
            missingNotificationHandler = ThrowMissingNotificationHandler()
        ) { registerNotification(h) }
        m.publish(PingNotification("ok"))
        assertEquals(listOf("ok"), h.received)
    }

    // ── SilentMissingNotificationHandlerStrategy ──────────────────────────────

    @Test
    fun `SilentMissingNotificationHandlerStrategy does not throw when no handlers registered`() = runTest {
        val m = mediator(
            missingNotificationHandler = SilentMissingNotificationHandler()
        ) { }
        m.publish(PingNotification("dropped"))
    }

    @Test
    fun `SilentMissingNotificationHandlerStrategy still delivers when handlers are registered`() = runTest {
        val h = RecordingNotificationHandler()
        val m = mediator(
            missingNotificationHandler = SilentMissingNotificationHandler()
        ) { registerNotification(h) }
        m.publish(PingNotification("ok"))
        assertEquals(listOf("ok"), h.received)
    }

    // ── NotificationPublishStrategy companion constants ────────────────────────

    @Test
    fun `DEFAULT companion constant delivers to all handlers`() = runTest {
        val h1 = RecordingNotificationHandler()
        val h2 = RecordingNotificationHandler()
        val pub = NotificationPublishStrategy.DEFAULT
        pub.publish(PingNotification("def"), listOf(h1, h2))
        assertEquals(listOf("def"), h1.received)
        assertEquals(listOf("def"), h2.received)
    }

    @Test
    fun `PARALLEL companion constant delivers to all handlers`() = runTest {
        val h = RecordingNotificationHandler()
        val pub = NotificationPublishStrategy.PARALLEL
        pub.publish(PingNotification("par"), listOf(h))
        assertEquals(listOf("par"), h.received)
    }

    @Test
    fun `SEQUENTIAL companion constant delivers in registration order`() = runTest {
        val order = mutableListOf<String>()
        val h1 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                order += "h1"
            }
        }
        val h2 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) {
                order += "h2"
            }
        }
        NotificationPublishStrategy.SEQUENTIAL.publish(PingNotification("x"), listOf(h1, h2))
        assertEquals(listOf("h1", "h2"), order)
    }

    @Test
    fun `CONTINUE_ON_EXCEPTION companion constant collects all failures`() = runTest {
        val h1 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) = throw RuntimeException("e1")
        }
        val h2 = object : NotificationHandler<PingNotification> {
            override suspend fun handle(notification: PingNotification) = throw RuntimeException("e2")
        }
        val ex = assertFailsWith<AggregateException> {
            NotificationPublishStrategy.CONTINUE_ON_EXCEPTION.publish(PingNotification("x"), listOf(h1, h2))
        }
        assertTrue(ex.message!!.contains("2"))
    }

    @Test
    fun `fireAndForget companion function returns FireAndForgetPublisher`() = runTest {
        val h = RecordingNotificationHandler()
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val pub = NotificationPublishStrategy.fireAndForget(scope)
        pub.publish(PingNotification("ff"), listOf(h))
        assertEquals(listOf("ff"), h.received)
    }
}
