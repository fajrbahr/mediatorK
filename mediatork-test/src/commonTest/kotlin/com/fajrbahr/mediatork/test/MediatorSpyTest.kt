package com.fajrbahr.mediatork.test

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MediatorSpyTest {

    private fun buildSpy(): MediatorSpy {
        val fake = FakeMediator {
            register(GetUserHandler())
            register(CreateOrderHandler())
            registerNotification(OrderPlacedHandler())
            registerNotification(object : com.fajrbahr.mediatork.api.NotificationHandler<UserDeletedEvent> {
                override suspend fun handle(notification: UserDeletedEvent) = Unit
            })
            registerStream(StreamItemsHandler())
        }
        return MediatorSpy(fake)
    }

    @Test
    fun `delegates send to the real mediator`() = runTest {
        val spy = buildSpy()
        assertEquals("user:42", spy.send(GetUserQuery("42")))
    }

    @Test
    fun `records sent requests in order`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.send(CreateOrderCommand("ORD-1"))
        spy.send(GetUserQuery("2"))
        assertEquals(3, spy.sentRequests.size)
    }

    @Test
    fun `sentOf filters by type`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.send(CreateOrderCommand("ORD-1"))
        spy.send(GetUserQuery("2"))
        assertEquals(2, spy.sentOf<GetUserQuery>().size)
        assertEquals(1, spy.sentOf<CreateOrderCommand>().size)
    }

    @Test
    fun `records published notifications`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.publish(UserDeletedEvent("U-1"))
        assertEquals(2, spy.publishedNotifications.size)
    }

    @Test
    fun `publishedOf filters by type`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.publish(OrderPlacedEvent("ORD-2"))
        spy.publish(UserDeletedEvent("U-1"))
        assertEquals(2, spy.publishedOf<OrderPlacedEvent>().size)
        assertEquals(1, spy.publishedOf<UserDeletedEvent>().size)
    }

    @Test
    fun `assertSent passes when request was sent`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.assertSent<GetUserQuery>()
    }

    @Test
    fun `assertSent fails when request was not sent`() = runTest {
        val spy = buildSpy()
        assertFailsWith<AssertionError> { spy.assertSent<GetUserQuery>() }
    }

    @Test
    fun `assertNotSent passes when request was not sent`() = runTest {
        val spy = buildSpy()
        spy.assertNotSent<GetUserQuery>()
    }

    @Test
    fun `assertNotSent fails when request was sent`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        assertFailsWith<AssertionError> { spy.assertNotSent<GetUserQuery>() }
    }

    @Test
    fun `assertPublished passes when notification was published`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.assertPublished<OrderPlacedEvent>()
    }

    @Test
    fun `assertPublished fails when notification was not published`() = runTest {
        val spy = buildSpy()
        assertFailsWith<AssertionError> { spy.assertPublished<OrderPlacedEvent>() }
    }

    @Test
    fun `assertNotPublished passes when notification was not published`() = runTest {
        val spy = buildSpy()
        spy.assertNotPublished<OrderPlacedEvent>()
    }

    @Test
    fun `assertNotPublished fails when notification was published`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        assertFailsWith<AssertionError> { spy.assertNotPublished<OrderPlacedEvent>() }
    }

    @Test
    fun `assertSentCount passes with exact count`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.send(GetUserQuery("2"))
        spy.assertSentCount<GetUserQuery>(2)
    }

    @Test
    fun `assertSentCount fails with wrong count`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        assertFailsWith<AssertionError> { spy.assertSentCount<GetUserQuery>(3) }
    }

    @Test
    fun `assertPublishedCount passes with exact count`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.publish(OrderPlacedEvent("ORD-2"))
        spy.assertPublishedCount<OrderPlacedEvent>(2)
    }

    @Test
    fun `assertPublishedCount fails with wrong count`() = runTest {
        val spy = buildSpy()
        spy.publish(OrderPlacedEvent("ORD-1"))
        assertFailsWith<AssertionError> { spy.assertPublishedCount<OrderPlacedEvent>(3) }
    }

    @Test
    fun `records streamed requests`() = runTest {
        val spy = buildSpy()
        spy.stream(StreamItemsQuery("a")).toList()
        assertEquals(1, spy.streamedRequests.size)
    }

    @Test
    fun `delegates stream to the real mediator`() = runTest {
        val spy = buildSpy()
        val items = spy.stream(StreamItemsQuery("item")).toList()
        assertEquals(listOf("item-1", "item-2", "item-3"), items)
    }

    @Test
    fun `streamedOf filters by type`() = runTest {
        val spy = buildSpy()
        spy.stream(StreamItemsQuery("a")).toList()
        assertEquals(1, spy.streamedOf<StreamItemsQuery>().size)
    }

    @Test
    fun `assertStreamed passes when stream was dispatched`() = runTest {
        val spy = buildSpy()
        spy.stream(StreamItemsQuery("a")).toList()
        spy.assertStreamed<StreamItemsQuery>()
    }

    @Test
    fun `assertStreamed fails when stream was not dispatched`() = runTest {
        val spy = buildSpy()
        assertFailsWith<AssertionError> { spy.assertStreamed<StreamItemsQuery>() }
    }

    @Test
    fun `assertNotStreamed passes when stream was not dispatched`() = runTest {
        val spy = buildSpy()
        spy.assertNotStreamed<StreamItemsQuery>()
    }

    @Test
    fun `assertNotStreamed fails when stream was dispatched`() = runTest {
        val spy = buildSpy()
        spy.stream(StreamItemsQuery("a")).toList()
        assertFailsWith<AssertionError> { spy.assertNotStreamed<StreamItemsQuery>() }
    }

    @Test
    fun `assertStreamedCount passes with exact count`() = runTest {
        val spy = buildSpy()
        spy.stream(StreamItemsQuery("a")).toList()
        spy.stream(StreamItemsQuery("b")).toList()
        spy.assertStreamedCount<StreamItemsQuery>(2)
    }

    @Test
    fun `reset clears all recorded calls`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        spy.publish(OrderPlacedEvent("ORD-1"))
        spy.stream(StreamItemsQuery("a")).toList()
        spy.reset()
        assertEquals(0, spy.sentRequests.size)
        assertEquals(0, spy.publishedNotifications.size)
        assertEquals(0, spy.streamedRequests.size)
    }

    @Test
    fun `spy preserves request values`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("user-99"))
        assertEquals("user-99", spy.sentOf<GetUserQuery>().first().id)
    }

    @Test
    fun `sentRequests returns defensive copy`() = runTest {
        val spy = buildSpy()
        spy.send(GetUserQuery("1"))
        val snapshot = spy.sentRequests
        spy.send(GetUserQuery("2"))
        assertEquals(1, snapshot.size)
        assertEquals(2, spy.sentRequests.size)
    }

    @Test
    fun `custom message appears in assertion error`() = runTest {
        val spy = buildSpy()
        val error = assertFailsWith<AssertionError> { spy.assertSent<GetUserQuery>("custom msg") }
        assertTrue(error.message!!.contains("custom msg"))
    }
}
