package dsl.meditor.products

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.Stage
import com.fajrbahr.mediatork.validator.ValidationResult

data class DynamicPriceQuery(val productId: String) : Request<Double>
data class PriceUpdatedEvent(val productId: String, val price: Double)

/**
 * Example: Runtime handler registration using the same DSL as feature blocks.
 * No need to create separate classes — use inline handler(), requestValidator(), etc.
 */
fun setupDynamicPricing(mediator: com.fajrbahr.mediatork.api.Mediator) {
    mediator.registry.scope {

        // Register handler inline (no class needed)
        handler<DynamicPriceQuery, Double> { request ->
            println("Fetching dynamic price for ${request.productId}")
            Math.random() * 100
        }

        // Register validator inline
        requestValidator<DynamicPriceQuery> { request ->
            if (request.productId.isBlank()) {
                ValidationResult.Invalid("Product ID required")
            } else {
                ValidationResult.Valid
            }
        }

        // Register notification handler inline
        notificationHandler<PriceUpdatedEvent> { event ->
            println("Price updated: ${event.productId} = $${event.price}")
        }

        // Register behavior inline
        behavior<DynamicPriceQuery, Double>(
            stage = Stage.Pre,
            order = 0
        ) { context, next, request ->
            println("[BEHAVIOR] Processing: ${request.productId}")
            next(request)
        }
    }
}
