package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.notification.NotificationHandler
import com.fajrbahr.mediatork.notification.otherwise
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FallbackNotificationHandlerTest {

    private class FailingHandler(private val message: String) : NotificationHandler<PingNotification> {
        override suspend fun handle(notification: PingNotification) = throw RuntimeException(message)
    }

    private class RecordingHandler(private val tag: String) : NotificationHandler<PingNotification> {
        val received = mutableListOf<String>()
        override suspend fun handle(notification: PingNotification) {
            received += tag
        }
    }

    @Test
    fun `uses primary handler when it succeeds`() = runTest {
        val primary = RecordingHandler("primary")
        val fallback = RecordingHandler("fallback")
        val m = mediator { registerNotification<PingNotification>(primary otherwise fallback) }
        m.publish(PingNotification("x"))
        assertEquals(listOf("primary"), primary.received)
        assertEquals(emptyList(), fallback.received)
    }

    @Test
    fun `falls back to second handler when first throws`() = runTest {
        val fallback = RecordingHandler("fallback")
        val m = mediator { registerNotification<PingNotification>(FailingHandler("oops") otherwise fallback) }
        m.publish(PingNotification("x"))
        assertEquals(listOf("fallback"), fallback.received)
    }

    @Test
    fun `chains three handlers and uses first successful one`() = runTest {
        val third = RecordingHandler("third")
        val m = mediator {
            registerNotification<PingNotification>(FailingHandler("1") otherwise FailingHandler("2") otherwise third)
        }
        m.publish(PingNotification("x"))
        assertEquals(listOf("third"), third.received)
    }

    @Test
    fun `rethrows last exception when all handlers fail`() = runTest {
        val m = mediator {
            registerNotification<PingNotification>(FailingHandler("first") otherwise FailingHandler("last"))
        }
        val ex = assertFailsWith<RuntimeException> { m.publish(PingNotification("x")) }
        assertEquals("last", ex.message)
    }

    @Test
    fun `plus DSL syntax works with otherwise chain`() = runTest {
        val fallback = RecordingHandler("fallback")
        val m = mediator { +(FailingHandler("oops") otherwise fallback) }
        m.publish(PingNotification("x"))
        assertEquals(listOf("fallback"), fallback.received)
    }
}
