package dsl.meditor.products

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.validator.rules

data class GetProductQuery(val productId: String) : Request<ProductDetails>
data class ProductDetails(val id: String, val name: String, val price: Double)

val productValidator = validator<GetProductQuery> { query ->
    rules<String> { check(query.productId.isNotBlank()) { "Product ID is required" } }
}

val getProductFeature = feature<GetProductQuery, ProductDetails> {
    validate(productValidator)
    handle { query ->
        ProductDetails(query.productId, "Widget Pro", 29.99)
    }
}
