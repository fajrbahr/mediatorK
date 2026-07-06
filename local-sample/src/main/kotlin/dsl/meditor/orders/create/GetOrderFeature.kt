package dsl.meditor.orders.create

import com.fajrbahr.mediatork.feature.feature
import kotlin.time.Duration.Companion.seconds

val getOrderFeature = feature<GetOrderQuery, OrderInfo> {
    handle { query ->
        println("Fetching order ${query.orderId}")
        OrderInfo(orderId = query.orderId, amount = 100.0)
    }
        .timeout(5.seconds)
        .cache(keyFrom = { it.orderId })
        .fallback { query ->
            // Fallback if primary handler times out or fails
            println("Using fallback for ${query.orderId}")
            OrderInfo(orderId = query.orderId, amount = 0.0)
        }
}
