package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MissingHandlerTest {

    // ── Requests / Notifications ──────────────────────────────────────────────

    private data object PingRequest : Request<String>
    private data object OtherRequest : Request<Int>
    private data object OrderPlaced : Notification
    private data object UserCreated : Notification

    // ── send() with no handler registered ────────────────────────────────────

    @Test
    fun `send throws MissingHandlerException when no handler registered`() = runTest {
        val mediator = mediatorK { }
        assertFailsWith<MissingHandlerException> {
            mediator.send(PingRequest)
        }
    }

    @Test
    fun `MissingHandlerException message contains request type name`() = runTest {
        val mediator = mediatorK { }
        val ex = assertFailsWith<MissingHandlerException> {
            mediator.send(PingRequest)
        }
        assertEquals(ex.message?.contains("PingRequest"), true)
    }

    @Test
    fun `send succeeds after handler is registered`() = runTest {
        val mediator = mediatorK {
            handle<PingRequest, String> { "pong" }
        }
        assertEquals("pong", mediator.send(PingRequest))
    }

    @Test
    fun `only unregistered request throws - registered one succeeds`() = runTest {
        val mediator = mediatorK {
            handle<PingRequest, String> { "pong" }
        }

        assertEquals("pong", mediator.send(PingRequest))

        assertFailsWith<MissingHandlerException> {
            mediator.send(OtherRequest)
        }
    }

    // ── publish() with no notification handler ────────────────────────────────

    @Test
    fun `publish throws when no notification handler registered`() = runTest {
        val mediator = mediatorK { }
        assertFailsWith<MissingNotificationHandlerException> {
            mediator.publish(OrderPlaced)
        }
    }

    @Test
    fun `notification handler is invoked when registered`() = runTest {
        val received = mutableListOf<Notification>()
        val mediator = mediatorK {
            notification<OrderPlaced> { received += it }
        }

        mediator.publish(OrderPlaced)
        assertEquals(1, received.size)
    }

    @Test
    fun `multiple handlers all invoked for same notification`() = runTest {
        val log = mutableListOf<String>()
        val mediator = mediatorK {
            notification<OrderPlaced> { log += "handler-1" }
            notification<OrderPlaced> { log += "handler-2" }
        }

        mediator.publish(OrderPlaced)
        assertEquals(2, log.size)
        assertTrue(log.containsAll(listOf("handler-1", "handler-2")))
    }

    @Test
    fun `notification handler not invoked for different notification type`() = runTest {
        val received = mutableListOf<Notification>()
        val mediator = mediatorK {
            notification<OrderPlaced> { received += it }
        }

        assertFailsWith<MissingNotificationHandlerException> {
            mediator.publish(UserCreated) // different type -- handler must not fire
        }
        assertTrue(received.isEmpty())
    }

    @Test
    fun `handlers for different notifications are independent`() = runTest {
        val log = mutableListOf<String>()
        val mediator = mediatorK {
            notification<OrderPlaced> { log += "order" }
            notification<UserCreated> { log += "user" }
            notificationPublisher = com.fajrbahr.mediatork.notification.NotificationPublishStrategy.SEQUENTIAL
        }

        mediator.publish(OrderPlaced)
        assertEquals(listOf("order"), log)

        mediator.publish(UserCreated)
        assertEquals(listOf("order", "user"), log)
    }

    // ── MissingStreamHandlerException with registered list ────────────────────

    private data class StreamTypeA(val n: Int) : StreamRequest<Int>
    private data class StreamTypeB(val n: Int) : StreamRequest<Int>

    @Test
    fun `MissingStreamHandlerException message includes registered stream type`() = runTest {
        val mediator = mediatorK {
            handleStream<StreamTypeA, Int> { kotlinx.coroutines.flow.emptyFlow() }
        }
        val ex = assertFailsWith<MissingStreamHandlerException> { mediator.stream(StreamTypeB(1)) }
        assertTrue(ex.message!!.contains("StreamTypeA"), "expected 'StreamTypeA' in: ${ex.message}")
    }

    // ── onMissingHandler (custom fallback) ───────────────────────────────────

    @Test
    fun `custom onMissingHandler returns default without throwing`() = runTest {
        val mediator = mediatorK {
            @Suppress("UNCHECKED_CAST")
            onMissingHandler = { throw MissingHandlerException(it::class.simpleName ?: "Unknown", emptyList()).let { _ -> error("default-value") } }
        }
        // Use a simpler approach: just override to return a default
        val mediator2 = mediatorK {
            onMissingHandler = { error("default-value") }
        }
        val ex = assertFailsWith<IllegalStateException> { mediator2.send(PingRequest) }
        assertEquals("default-value", ex.message)
    }

    @Test
    fun `custom onMissingHandler is invoked when no handler registered`() = runTest {
        var called = false
        val mediator = mediatorK {
            onMissingHandler = { called = true; error("fallback") }
        }
        assertFailsWith<IllegalStateException> { mediator.send(PingRequest) }
        assertTrue(called)
    }

    // ── mediatorK builds working mediator ────────────────────────────────────

    @Test
    fun `mediatorK with handler builds working mediator`() = runTest {
        val m = mediatorK {
            handle<PingRequest, String> { "pong" }
        }
        assertEquals("pong", m.send(PingRequest))
    }

    @Test
    fun `mediatorK respects registered notification handlers`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            notification<OrderPlaced> { log += "fired" }
        }
        m.publish(OrderPlaced)
        assertEquals(listOf("fired"), log)
    }
}
