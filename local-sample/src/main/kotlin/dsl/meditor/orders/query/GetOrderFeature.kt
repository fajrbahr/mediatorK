package dsl.meditor.orders.create

import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.measure
import dsl.meditor.showcase.PaymentResult
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val getOrderFeature = feature<GetOrderQuery, OrderInfo> {
    handle { query ->
        println("Fetching order ${query.orderId}")
        OrderInfo(orderId = query.orderId, amount = 100.0)
    }
        .retry(maxAttempts = 3)
        // Extension: timeout protection
        .timeout(5000.milliseconds)
        // Extension: result caching
        .cache(keyFrom = { it.orderId })
        // Extension: fallback on failure
        .fallback { query ->
            // Fallback if primary handler times out or fails
            println("Using fallback for ${query.orderId}")
            OrderInfo(orderId = query.orderId, amount = 0.0)
        }
        // Extension: execution timing
        .measure()

}
