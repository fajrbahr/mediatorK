package dsl.meditor.orders.create

import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.mediatorModule
import dsl.meditor.context.locale
import kotlin.time.Duration.Companion.seconds

data class CreateOrderCommandRaw(
    val id: String,
    val amount: Double,
) : com.fajrbahr.mediatork.api.Request<OrderResult>

val orderFeatureWithoutUI = handler<CreateOrderCommandRaw, OrderResult> {

    val newOrderId = "ORD-${it.id}"
    println("Creating order $newOrderId with locale ${context.locale}")

    publish(
        OrderCreatedNotification(
            orderId = newOrderId,
            customerEmail = "customer@example.com",
            customerPhone = "+1234567890",
            totalAmount = it.amount,
        )
    )

    OrderResult(orderId = newOrderId, responseTime = 0)
}


val orderSliceWithoutUI2 = mediatorModule {
  add(orderFeatureWithoutUI)
}
