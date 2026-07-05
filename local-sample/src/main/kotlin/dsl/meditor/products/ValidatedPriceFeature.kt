package dsl.meditor.products

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

data class ValidatedGetPriceQuery(val productId: String) : Request<Double>

/**
 * Example: Feature with DSL-based validators using requestValidator().
 * Shows the fluent, inline approach to validation - no separate validator classes needed.
 */
val PriceInlineFeature = feature<ValidatedGetPriceQuery, Double> {

    // Validator 1: DSL-based inline validation
    requestValidator { request ->
        rules<String> {
            require(request.productId.isNotBlank()) { "Product ID is required" }
            require(request.productId.length >= 3) { "Product ID must be at least 3 chars" }
            check(request.productId.matches(Regex("[A-Z0-9-]+"))) { "Invalid product ID format" }
        }
    }

    // Validator 2: Multiple validators stack automatically
    requestValidator { request ->
        when {
            request.productId.startsWith("TEST-") -> ValidationResult.Valid
            request.productId.startsWith("DEMO-") ->
                ValidationResult.ValidWithWarnings(warnings = listOf("Demo product - limited availability"))

            else -> ValidationResult.Valid
        }
    }

    // Handler
    requestHandler { request ->
        println("Getting price for validated product: ${request.productId}")
        99.99
    }

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
