package dsl.meditor.orders.create

import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.mapper
import dsl.meditor.context.locale

val orderFeature = feature<CreateOrderCommand, OrderResult, OrderUi> {
    validate {
        check(request.amount > 0, "Amount must be positive")
        check(request.amount <= 10_000, "Amount must not exceed 10,000")
        warn(request.amount > 1_000, "High-value order — requires manager approval")
        warn(request.amount > 5_000, "Very high-value order — triggers fraud review")
    }

    before { ctx, request ->
        println("Before: ${request::class.simpleName}")
    }

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
    }.retry().cache()

    after { ctx, result, request ->
        println("After: $result")
    }

    mapper<OrderResult, OrderUi> { raw ->
        OrderUi(orderId = raw.orderId)
    }
}
