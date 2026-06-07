package sample.command

import com.fajrbahr.mediatork.*
import kotlinx.coroutines.delay
import sample.context.addTraceMeta
import sample.context.currentUser
import sample.context.locale
import sample.notification.OrderCreatedNotification
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

data class OrderResult(
    val orderId: String = "",
    val cart: List<String> = emptyList(),
    val responseIme: Long
)

data class CreateOrderCommand(
    val id: String, val amount: Double
) : Request<OrderResult>


class CreateOrderHandler(
    val api: FakeApi,
) : RequestHandler<CreateOrderCommand, OrderResult> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand
    ): OrderResult {
        val newOrderId = "ORD-$4234234}"

        val result = api.createOrder(newOrderId, requestContext.locale, requestContext.currentUser!!.token)

        // Publish notification trigger
        // UpdateInventoryHandler
        // SendOrderSmsHandler
        // SendOrderConfirmationEmailHandler
        // TrackOrderAnalyticsHandler

        mediator.publish(
            OrderCreatedNotification(
                orderId = newOrderId, customerEmail = requestContext.currentUser!!.email,
                customerPhone = "+1234567890",
                totalAmount = request.amount
            )
        )

        val metrics1 = Pair("networking", result.responseIme)
        val metrics2 = Pair("dbWrite", 2342344L)

        requestContext.addTraceMeta(listOf(metrics1, metrics2))

        return result
    }
}

class OrderRegistrar(val fakeApi: FakeApi = FakeApi()) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateOrderHandler(api = fakeApi)
        }
    }
}


class FakeApi {
    suspend fun createOrder(newOrderId: String, locale: String, email: String): OrderResult {
        val duration = measureTime {
            delay(3.seconds)
            println("Creating order $newOrderId for $email with locale $locale")
        }

        return OrderResult(responseIme = duration.inWholeSeconds)
    }
}
