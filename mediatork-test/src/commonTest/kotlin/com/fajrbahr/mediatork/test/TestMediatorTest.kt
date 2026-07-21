package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.MissingHandlerException
import com.fajrbahr.mediatork.behavior
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.validator.ValidationException
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestMediatorTest {

    @Test
    fun `handler returns value`() = runTest {
        val mediator = testMediator {
            handle<GetUserQuery, String> { "user:${it.id}" }
        }
        assertEquals("user:42", mediator.send(GetUserQuery("42")))
    }

    @Test
    fun `handler throws error`() = runTest {
        val mediator = testMediator {
            handle<GetUserQuery, String> { error("not found") }
        }
        assertFailsWith<IllegalStateException> {
            mediator.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `handler answers dynamically`() = runTest {
        val mediator = testMediator {
            handle<GetUserQuery, String> { "user:${it.id}" }
        }
        assertEquals("user:99", mediator.send(GetUserQuery("99")))
    }

    @Test
    fun `missing handler throws`() = runTest {
        val mediator = testMediator { }
        assertFailsWith<MissingHandlerException> {
            mediator.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `supports multiple handlers`() = runTest {
        val mediator = testMediator {
            handle<GetUserQuery, String> { "user" }
            handle<CreateOrderCommand, String> { "order" }
        }
        assertEquals("user", mediator.send(GetUserQuery("1")))
        assertEquals("order", mediator.send(CreateOrderCommand("1")))
    }

    @Test
    fun `supports Unit requests`() = runTest {
        val mediator = testMediator {
            handle<DeleteOrderCommand, Unit> { }
        }
        mediator.send(DeleteOrderCommand("1"))
    }

    @Test
    fun `records sent requests`() = runTest {
        val mediator = testMediator {
            handle<GetUserQuery, String> { "user" }
        }
        mediator.send(GetUserQuery("1"))
        mediator.send(GetUserQuery("2"))
        assertEquals(2, mediator.sentOf<GetUserQuery>().size)
    }

    @Test
    fun `notification handler is invoked and recorded`() = runTest {
        val captured = mutableListOf<String>()
        val mediator = testMediator {
            notification<OrderPlacedEvent> { captured += it.orderId }
        }
        mediator.publish(OrderPlacedEvent("ORD-1"))
        assertEquals(listOf("ORD-1"), captured)
        assertEquals(1, mediator.publishedOf<OrderPlacedEvent>().size)
    }

    @Test
    fun `notification handler can throw`() = runTest {
        val mediator = testMediator {
            notification<OrderPlacedEvent> { error("fail") }
        }
        assertFailsWith<IllegalStateException> {
            mediator.publish(OrderPlacedEvent("ORD-1"))
        }
    }

    @Test
    fun `stream handler returns items`() = runTest {
        val mediator = testMediator {
            handleStream<StreamItemsQuery, String> { flowOf("a", "b", "c") }
        }
        val items = mediator.stream(StreamItemsQuery("x")).toList()
        assertEquals(listOf("a", "b", "c"), items)
    }

    @Test
    fun `stream handler can throw`() = runTest {
        val mediator = testMediator {
            handleStream<StreamItemsQuery, String> { flow { error("fail") } }
        }
        assertFailsWith<IllegalStateException> {
            mediator.stream(StreamItemsQuery("x")).toList()
        }
    }

    @Test
    fun `stream handler answers dynamically`() = runTest {
        val mediator = testMediator {
            handleStream<StreamItemsQuery, String> { req -> flow { emit("${req.prefix}-dynamic") } }
        }
        val items = mediator.stream(StreamItemsQuery("x")).toList()
        assertEquals(listOf("x-dynamic"), items)
    }

    @Test
    fun `publish with custom strategy invokes handler`() = runTest {
        val captured = mutableListOf<String>()
        val mediator = testMediator {
            notification<OrderPlacedEvent> { captured += it.orderId }
        }
        mediator.publish(OrderPlacedEvent("ORD-1"), NotificationPublishStrategy.PARALLEL)
        assertEquals(listOf("ORD-1"), captured)
    }

    @Test
    fun `behaviors wrap the handler`() = runTest {
        val log = mutableListOf<String>()
        val mediator = testMediator {
            handle<GetUserQuery, String> { "user" }
            behaviors(
                behavior { _, _, next ->
                    log += "before"
                    next().also { log += "after" }
                }
            )
        }
        assertEquals("user", mediator.send(GetUserQuery("1")))
        assertEquals(listOf("before", "after"), log)
    }

    @Test
    fun `behaviors run in order`() = runTest {
        val log = mutableListOf<String>()
        val mediator = testMediator {
            handle<GetUserQuery, String> { "user" }
            behaviors(
                behavior(order = 10) { _, _, next -> log += "second"; next() },
                behavior(order = -10) { _, _, next -> log += "first"; next() },
            )
        }
        mediator.send(GetUserQuery("1"))
        assertEquals(listOf("first", "second"), log)
    }

    @Test
    fun `validators reject invalid requests`() = runTest {
        val mediator = testMediator {
            handle<GetUserQuery, String> { "user" }
            validate<GetUserQuery> { req ->
                if (req.id.isBlank()) ValidationResult.Invalid("id required") else ValidationResult.Valid
            }
        }
        assertEquals("user", mediator.send(GetUserQuery("42")))
        assertFailsWith<ValidationException> {
            mediator.send(GetUserQuery(""))
        }
    }

    @Test
    fun `RecordingMediator records around an existing mediator`() = runTest {
        val mediator = RecordingMediator(
            com.fajrbahr.mediatork.mediatorK {
                handle<GetUserQuery, String> { "user:${it.id}" }
            }
        )
        assertEquals("user:7", mediator.send(GetUserQuery("7")))
        assertTrue(mediator.sent.isNotEmpty())
    }
}
