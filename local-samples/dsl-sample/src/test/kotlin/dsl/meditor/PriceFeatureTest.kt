package dsl.meditor

import com.fajrbahr.mediatork.test.*
import com.fajrbahr.mediatork.validator.ValidationException
import dsl.meditor.products.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PriceFeatureTest {

    private val testRepo = object : PriceRepo {
        override fun findPrice(productId: String) = RawPrice(productId, cents = 1999)
    }

    private val testPushService = object : PushService {
        val sent = mutableListOf<String>()
        override fun send(orderId: String, phone: String) { sent += orderId }
    }

    private val testInAppService = object : InAppService {
        val notified = mutableListOf<String>()
        override fun notify(orderId: String) { notified += orderId }
    }

    @Test
    fun `get price maps raw cents to formatted display`() = runTest {
        val mediator = FakeMediator {
            +getPriceFeature(testRepo, testPushService, testInAppService)
        }

        val price = mediator.send(GetPriceQuery(productId = "PROD-1"))

        assertEquals("$19.99", price.display)
    }

    @Test
    fun `validation rejects blank product ID`() = runTest {
        val mediator = FakeMediator {
            +getPriceFeature(testRepo, testPushService, testInAppService)
        }

        assertFailsWith<ValidationException> {
            mediator.send(GetPriceQuery(productId = ""))
        }
    }

    @Test
    fun `spy tracks price queries`() = runTest {
        val fake = FakeMediator {
            +getPriceFeature(testRepo, testPushService, testInAppService)
        }
        val spy = MediatorSpy(fake)

        spy.send(GetPriceQuery(productId = "PROD-A"))
        spy.send(GetPriceQuery(productId = "PROD-B"))

        spy.assertSent<GetPriceQuery>()
        spy.assertSentCount<GetPriceQuery>(2)
        assertEquals("PROD-A", spy.sentOf<GetPriceQuery>().first().productId)
    }

    @Test
    fun `capture notifications from feature`() = runTest {
        val mediator = FakeMediator {
            +getPriceFeature(testRepo, testPushService, testInAppService)
        }
        val captured = mediator.captureNotifications<OrderCreatedNotification>()

        mediator.publish(
            OrderCreatedNotification(
                orderId = "ORD-1",
                customerEmail = "test@test.com",
                customerPhone = "+1234",
                totalAmount = 19.99,
            )
        )

        assertEquals(1, captured.size)
        assertEquals("ORD-1", captured.first().orderId)
    }

    @Test
    fun `harness given-send-query pattern with price feature`() = runTest {
        val harness = buildHandlerTestHarness {
            +getPriceFeature(testRepo, testPushService, testInAppService)
        }

        val price = harness.send(GetPriceQuery(productId = "PROD-X"))

        assertEquals("$19.99", price.display)
    }

    @Test
    fun `dummy mediator satisfies dependency without real handlers`() {
        val mediator: com.fajrbahr.mediatork.api.Mediator = DummyMediator()
        assertNotNull(mediator)
    }

    @Test
    fun `dummy mediator publish does nothing`() = runTest {
        val mediator = DummyMediator()
        mediator.publish(
            OrderCreatedNotification("ORD-1", "a@b.com", "+1", 10.0)
        )
    }
}
