package sample.orders.commands.createorder

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.RequestHandler
import kotlinx.coroutines.delay
import sample.context.addTraceMeta
import sample.context.currentUser
import sample.context.locale
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class CreateOrderHandler(
    val api: FakeApi,
) : RequestHandler<CreateOrderCommand, OrderResult> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand,
    ): OrderResult {
        val newOrderId = "ORD-${request.id}"

        val result = api.createOrder(newOrderId, requestContext.locale, requestContext.currentUser!!.token)

        mediator.publish(
            OrderCreatedNotification(
                orderId = newOrderId,
                customerEmail = requestContext.currentUser!!.email,
                customerPhone = "+1234567890",
                totalAmount = request.amount,
            )
        )

        val metrics1 = Pair("networking", result.responseIme)
        val metrics2 = Pair("dbWrite", 2342344L)
        requestContext.addTraceMeta(listOf(metrics1, metrics2))

        return result
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

class OrderRegistrar(val fakeApi: FakeApi = FakeApi()) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateOrderHandler(api = fakeApi)
        }
    }
}
