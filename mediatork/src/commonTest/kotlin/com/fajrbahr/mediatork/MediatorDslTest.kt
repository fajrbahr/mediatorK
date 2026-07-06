package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.validator.ValidationException
import com.fajrbahr.mediatork.validator.collectingValidator
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediatorDslTest {

    private data class NumbersQuery(val count: Int) : StreamRequest<Int>

    @Test
    fun `mediatorK with lambda handler dispatches send`() = runTest {
        val m = buildMediatorK {
            handle<PingQuery, String> { request -> "pong:${request.value}" }
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `lambda handler can publish through HandlerScope`() = runTest {
        val received = mutableListOf<String>()
        val m = buildMediatorK {
            handle<PingQuery, String> { request ->
                publish(PingNotification(request.value))
                "done"
            }
            on<PingNotification> { notification -> received += notification.message }
        }
        m.send(PingQuery("event"))
        assertEquals(listOf("event"), received)
    }

    @Test
    fun `on registers multiple notification handlers`() = runTest {
        val calls = mutableListOf<Int>()
        val m = buildMediatorK {
            handle<PingQuery, String> { "ok" }
            notificationPublisher = NotificationPublishStrategy.SequentialNotificationPublisher()
            on<AlertNotification>(order = 2) { calls += it.level * 10 }
            on<AlertNotification>(order = 1) { calls += it.level }
        }
        m.publish(AlertNotification(3))
        assertEquals(listOf(3, 30), calls)
    }

    @Test
    fun `validate lambda rejects invalid request`() = runTest {
        val m = buildMediatorK {
            handle<AddCommand, Int> { request -> request.a + request.b }
            validate<AddCommand> { request ->
                collectingValidator {
                    check(request.a >= 0) { "a must be non-negative" }
                }
            }
        }
        assertEquals(5, m.send(AddCommand(2, 3)))
        assertFailsWith<ValidationException> { m.send(AddCommand(-1, 3)) }
    }

    @Test
    fun `handleStream lambda dispatches stream`() = runTest {
        val m = buildMediatorK {
            verifyHandlers = false
            handleStream<NumbersQuery, Int> { request -> (1..request.count).asFlow() }
        }
        assertEquals(listOf(1, 2, 3), m.stream(NumbersQuery(3)).toList())
    }

    @Test
    fun `class-based handlers and registrars mix with lambdas`() = runTest {
        val m = buildMediatorK {
            handler(PingHandler())
            +AddHandler()
            handle<EchoQuery, String> { request -> request.text }
        }
        assertEquals("pong:x", m.send(PingQuery("x")))
        assertEquals(7, m.send(AddCommand(3, 4)))
        assertEquals("echo", m.send(EchoQuery("echo")))
    }

    @Test
    fun `missing handler still throws with builder`() = runTest {
        val m = buildMediatorK { }
        assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
    }
}
