package dsl.meditor.products

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.mappedFeature
import com.fajrbahr.mediatork.feature.mapper

data class GetPriceQuery(val productId: String) : Request<FormattedPrice>
data class RawPrice(val cents: Int)
data class FormattedPrice(val display: String)

val priceMapper = mapper<RawPrice, FormattedPrice> { raw ->
    FormattedPrice("$${raw.cents / 100}.${"%02d".format(raw.cents % 100)}")
}

val getPriceFeature = mappedFeature<GetPriceQuery, RawPrice, FormattedPrice> {
    handle { query ->
        RawPrice(cents = 2999)
    }
    map(priceMapper)
}
