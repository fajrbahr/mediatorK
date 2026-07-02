package dsl.meditor.products

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.*
import com.fajrbahr.mediatork.validator.rules

data class GetPriceQuery(val productId: String) : Request<FormattedPrice>

//Domain Model
data class RawPrice(val productId: String, val cents: Int)

// Ui model
data class FormattedPrice(val display: String)

val priceMapper = mapper<RawPrice, FormattedPrice> { raw ->
    FormattedPrice("$${raw.cents / 100}.${"%02d".format(raw.cents % 100)}")
}
val priceValidator = validator<GetPriceQuery> { query ->
    rules<String> { check(query.productId.isNotBlank()) { "Product ID is required" } }
}

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

interface PriceRepo {
    fun findPrice(productId: String): RawPrice
}

interface PushService {
    fun send(orderId: String, phone: String)
}

interface InAppService {
    fun notify(orderId: String)
}

fun sendOrderPushHandler(pushService: PushService) = notification<OrderCreatedNotification> {
    pushService.send(it.orderId, it.customerPhone)
}

fun sendInAppHandler(inAppService: InAppService) = notification<OrderCreatedNotification> {
    inAppService.notify(it.orderId)
}

fun priceHandler(repo: PriceRepo) = handler<GetPriceQuery, RawPrice> {
    repo.findPrice(it.productId)
}

fun getPriceFeature(
    repo: PriceRepo,
    pushService: PushService,
    inAppService: InAppService,
) = mappedFeature<GetPriceQuery, FormattedPrice>(priceMapper) {
    validate(priceValidator)

    notification(sendOrderPushHandler(pushService))
    notification(sendInAppHandler(inAppService))

    handler(priceHandler(repo))
}
