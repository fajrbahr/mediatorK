package dsl.meditor.orders.stream

import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.mediatorModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * Stream feature example: Using the feature builder for stream requests.
 * This demonstrates the feature<StreamRequest<T>, T> overload.
 */
data class RealtimeOrderPriceStream(
    val orderId: String,
) : StreamRequest<PriceUpdate>

data class PriceUpdate(
    val orderId: String,
    val currentPrice: Double,
)

val realtimeOrderPriceFeature = feature<RealtimeOrderPriceStream, PriceUpdate> {
    handle { request ->
        flow {
            var price = 100.0
            repeat(5) {
                delay(200)
                price += (Math.random() * 10 - 5)
                emit(PriceUpdate(orderId = request.orderId, currentPrice = price))
            }
        }
    }
}

val orderUpdatesStreamFeatureSlice = mediatorModule {
    add(realtimeOrderPriceFeature)
}
