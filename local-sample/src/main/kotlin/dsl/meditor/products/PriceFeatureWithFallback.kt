package dsl.meditor.products

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.handler.handler

data class GetPriceWithFallbackQuery(val productId: String) : Request<Double>

/**
 * Example: Feature with handler fallback using `otherwise`.
 * Primary handler tries to get price from database.
 * If it fails, fallback handler returns a default/cached price.
 */
val getPriceWithFallbackFeature = feature<GetPriceWithFallbackQuery, Double> {

    val primaryHandler = handler<GetPriceWithFallbackQuery, Double> { request ->
        // Try primary source (e.g., database)
        if (request.productId.startsWith("PREMIUM")) {
            throw Exception("Premium product pricing unavailable")
        }
        println("[PRIMARY] Getting price for ${request.productId}")
        99.99
    }

    val fallbackHandler = handler<GetPriceWithFallbackQuery, Double> { request ->
        // Fallback to cache or default
        println("[FALLBACK] Using cached price for ${request.productId}")
        49.99
    }

    // Chain handlers with otherwise - used at runtime
    // val chainedHandler = primaryHandler otherwise fallbackHandler
    // This pattern works great for:
    // - Feature registration: register(primaryHandler otherwise fallbackHandler)
    // - Runtime setup: mediator.registry.register(primaryHandler otherwise fallbackHandler)

    requestHandler { request ->
        // Or implement fallback logic inline
        try {
            if (request.productId.startsWith("PREMIUM")) {
                throw Exception("Unavailable")
            }
            println("[INLINE] Getting price for ${request.productId}")
            99.99
        } catch (e: Exception) {
            println("[INLINE FALLBACK] Using cached price")
            49.99
        }
    }
}
