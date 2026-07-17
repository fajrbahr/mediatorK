package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.behavior
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StubMediatorTest {

    private val mediator = StubMediator()

    @Test
    fun `returns stubbed value`() = runTest {
        mediator.on<GetUserQuery>() returns "user:42"
        assertEquals("user:42", mediator.send(GetUserQuery("42")))
    }

    @Test
    fun `throws stubbed error`() = runTest {
        mediator.on<GetUserQuery>() throws IllegalStateException("not found")
        assertFailsWith<IllegalStateException> {
            mediator.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `answers with dynamic response`() = runTest {
        mediator.on<GetUserQuery>() answers { "user:${it.id}" }
        assertEquals("user:99", mediator.send(GetUserQuery("99")))
    }

    @Test
    fun `throws when no stub registered`() = runTest {
        assertFailsWith<IllegalStateException> {
            mediator.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `supports multiple stubs`() = runTest {
        mediator.on<GetUserQuery>() returns "user"
        mediator.on<CreateOrderCommand>() returns "order"
        assertEquals("user", mediator.send(GetUserQuery("1")))
        assertEquals("order", mediator.send(CreateOrderCommand("1")))
    }

    @Test
    fun `supports Unit requests`() = runTest {
        mediator.on<DeleteOrderCommand>() returns Unit
        mediator.send(DeleteOrderCommand("1"))
    }

    @Test
    fun `records sent requests`() = runTest {
        mediator.on<GetUserQuery>() returns "user"
        mediator.send(GetUserQuery("1"))
        mediator.send(GetUserQuery("2"))
        assertEquals(2, mediator.sentOf<GetUserQuery>().size)
    }

    @Test
    fun `onNotification invokes stub`() = runTest {
        val captured = mutableListOf<String>()
        mediator.onNotification<OrderPlacedEvent>() answers { captured += it.orderId }
        mediator.publish(OrderPlacedEvent("ORD-1"))
        assertEquals(listOf("ORD-1"), captured)
        assertEquals(1, mediator.published.size)
    }

    @Test
    fun `onNotification throws stubbed error`() = runTest {
        mediator.onNotification<OrderPlacedEvent>() throws IllegalStateException("fail")
        assertFailsWith<IllegalStateException> {
            mediator.publish(OrderPlacedEvent("ORD-1"))
        }
    }

    @Test
    fun `publish is no-op when no notification stub registered`() = runTest {
        mediator.publish(OrderPlacedEvent("ORD-1"))
    }

    @Test
    fun `onStream returns stubbed items`() = runTest {
        mediator.onStream<StreamItemsQuery>() returns listOf("a", "b", "c")
        val items = mediator.stream(StreamItemsQuery("x")).toList()
        assertEquals(listOf("a", "b", "c"), items)
    }

    @Test
    fun `onStream throws stubbed error`() = runTest {
        mediator.onStream<StreamItemsQuery>() throws IllegalStateException("fail")
        assertFailsWith<IllegalStateException> {
            mediator.stream(StreamItemsQuery("x")).toList()
        }
    }

    @Test
    fun `onStream answers with dynamic flow`() = runTest {
        mediator.onStream<StreamItemsQuery>() answers { flow { emit("${it.prefix}-dynamic") } }
        val items = mediator.stream(StreamItemsQuery("x")).toList()
        assertEquals(listOf("x-dynamic"), items)
    }

    @Test
    fun `stream returns empty flow when no stub registered`() = runTest {
        val items = mediator.stream(StreamItemsQuery("x")).toList()
        assertTrue(items.isEmpty())
    }

    @Test
    fun `publish with custom strategy uses notification stub`() = runTest {
        val captured = mutableListOf<String>()
        mediator.onNotification<OrderPlacedEvent>() answers { captured += it.orderId }
        mediator.publish(OrderPlacedEvent("ORD-1"), NotificationPublishStrategy.PARALLEL)
        assertEquals(listOf("ORD-1"), captured)
    }

    @Test
    fun `last stub wins for same request type`() = runTest {
        mediator.on<GetUserQuery>() returns "first"
        mediator.on<GetUserQuery>() returns "second"
        assertEquals("second", mediator.send(GetUserQuery("1")))
    }

    @Test
    fun `onPipeline runs behavior`() = runTest {
        val log = mutableListOf<String>()
        mediator.onPipeline(behavior { _, _, next ->
            log += "before"
            next().also { log += "after" }
        })
        mediator.on<GetUserQuery>() returns "user"
        assertEquals("user", mediator.send(GetUserQuery("1")))
        assertEquals(listOf("before", "after"), log)
    }

    @Test
    fun `onPipeline toggle on and off`() = runTest {
        val log = mutableListOf<String>()
        val pip = mediator.onPipeline(behavior { _, _, next ->
            log += "hit"
            next()
        })
        mediator.on<GetUserQuery>() returns "user"

        mediator.send(GetUserQuery("1"))
        assertEquals(1, log.size)

        pip.enabled = false
        mediator.send(GetUserQuery("2"))
        assertEquals(1, log.size)

        pip.enabled = true
        mediator.send(GetUserQuery("3"))
        assertEquals(2, log.size)
    }

    @Test
    fun `onPipeline respects order`() = runTest {
        val log = mutableListOf<String>()
        mediator.onPipeline(behavior(order = 10) { _, _, next ->
            log += "second"
            next()
        })
        mediator.onPipeline(behavior(order = -10) { _, _, next ->
            log += "first"
            next()
        })
        mediator.on<GetUserQuery>() returns "user"
        mediator.send(GetUserQuery("1"))
        assertEquals(listOf("first", "second"), log)
    }

    @Test
    fun `pipelineEnabled toggles all behaviors`() = runTest {
        val log = mutableListOf<String>()
        mediator.onPipeline(behavior { _, _, next ->
            log += "a"
            next()
        })
        mediator.onPipeline(behavior { _, _, next ->
            log += "b"
            next()
        })
        mediator.on<GetUserQuery>() returns "user"

        mediator.send(GetUserQuery("1"))
        assertEquals(listOf("a", "b"), log)

        log.clear()
        mediator.pipelineEnabled = false
        mediator.send(GetUserQuery("2"))
        assertTrue(log.isEmpty())

        mediator.pipelineEnabled = true
        mediator.send(GetUserQuery("3"))
        assertEquals(listOf("a", "b"), log)
    }
}
