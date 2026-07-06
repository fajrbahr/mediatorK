package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DummyMediatorTest {

    @Test
    fun `send returns Unit cast to the result type`() = runTest {
        val dummy = DummyMediator()
        val result = dummy.send(DeleteOrderCommand("id-1"))
        assertEquals(Unit, result)
    }

    @Test
    fun `publish does nothing and does not throw`() = runTest {
        val dummy = DummyMediator()
        dummy.publish(OrderPlacedEvent("ORD-1"))
    }

    @Test
    fun `publish with strategy does nothing and does not throw`() = runTest {
        val dummy = DummyMediator()
        dummy.publish(OrderPlacedEvent("ORD-1"), NotificationPublishStrategy.ParallelNotificationPublisher())
    }

    @Test
    fun `stream returns empty flow`() = runTest {
        val dummy = DummyMediator()
        val items = dummy.stream(StreamItemsQuery("x")).toList()
        assertTrue(items.isEmpty())
    }

    @Test
    fun `multiple sends do not interfere`() = runTest {
        val dummy = DummyMediator()
        dummy.send(DeleteOrderCommand("a"))
        dummy.send(DeleteOrderCommand("b"))
        dummy.send(DeleteOrderCommand("c"))
    }
}
