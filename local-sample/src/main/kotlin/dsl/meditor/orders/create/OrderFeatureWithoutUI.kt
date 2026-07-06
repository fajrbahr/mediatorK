package dsl.meditor.orders.create

import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.mediatorModule
import dsl.meditor.context.locale
import kotlin.time.Duration.Companion.seconds

data class CreateOrderCommandRaw(
    val id: String,
    val amount: Double,
) : com.fajrbahr.mediatork.api.Request<OrderResult>

val orderFeatureWithoutUI = feature<CreateOrderCommandRaw, OrderResult> {
    handle { request ->
        val newOrderId = "ORD-${request.id}"
        println("Creating order $newOrderId with locale ${context.locale}")

        publish(
            OrderCreatedNotification(
                orderId = newOrderId,
                customerEmail = "customer@example.com",
                customerPhone = "+1234567890",
                totalAmount = request.amount,
            )
        )

        OrderResult(orderId = newOrderId, responseTime = 0)
    }
        .timeout(10.seconds)
        .retry(2)
}

val orderSliceWithoutUI = mediatorModule {
    feature(orderFeatureWithoutUI)
}
