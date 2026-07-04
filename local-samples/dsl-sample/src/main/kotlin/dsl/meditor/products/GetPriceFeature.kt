package dsl.meditor.products

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.feature.*
import com.fajrbahr.mediatork.validator.rules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// ── Request & Models ─────────────────────────────────────────────────────────

data class GetPriceQuery(val productId: String) : Request<FormattedPrice>

data class RawPrice(val productId: String, val cents: Int)

data class FormattedPrice(val display: String)

// ── Mapper ───────────────────────────────────────────────────────────────────

val priceMapper = mapper<RawPrice, FormattedPrice> { raw ->
    FormattedPrice("$${raw.cents / 100}.${"%02d".format(raw.cents % 100)}")
}

// ── Multiple Validators ──────────────────────────────────────────────────────

val priceValidator = validator<GetPriceQuery> { query ->
    rules<String> { check(query.productId.isNotBlank()) { "Product ID is required" } }
}

val priceFormatValidator = validator<GetPriceQuery> { query ->
    rules<String> { check(query.productId.startsWith("PROD-")) { "Product ID must start with PROD-" } }
}

// ── Notification ─────────────────────────────────────────────────────────────

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

// ── Dependencies ─────────────────────────────────────────────────────────────

interface PriceRepo {
    fun findPrice(productId: String): RawPrice
}

interface PushService {
    fun send(orderId: String, phone: String)
}

interface InAppService {
    fun notify(orderId: String)
}

// ── Notification Handlers ────────────────────────────────────────────────────

fun sendOrderPushHandler(pushService: PushService) = notification<OrderCreatedNotification> {
    pushService.send(it.orderId, it.customerPhone)
}

fun sendInAppHandler(inAppService: InAppService) = notification<OrderCreatedNotification> {
    inAppService.notify(it.orderId)
}

// ── Handler ──────────────────────────────────────────────────────────────────

fun priceHandler(repo: PriceRepo) = handler<GetPriceQuery, RawPrice> {
    repo.findPrice(it.productId)
}

// ── Mapped Feature (with multiple validators + bundled behavior) ─────────────

fun getPriceFeature(
    repo: PriceRepo,
    pushService: PushService,
    inAppService: InAppService,
) = mappedFeature<GetPriceQuery, FormattedPrice>(priceMapper) {
    validate(priceValidator)
    validate(priceFormatValidator)

    behavior(stage = Stage.Pre, order = -1) { ctx, next, request ->
        println("  [PRODUCT] Accessing: ${request::class.simpleName}")
        next(request)
    }

    notification(sendOrderPushHandler(pushService))
    notification(sendInAppHandler(inAppService))

    handler(priceHandler(repo))
}

// ── Stream Feature ───────────────────────────────────────────────────────────

data class PriceUpdate(val productId: String, val oldCents: Int, val newCents: Int)

data class WatchPriceQuery(val productId: String) : StreamRequest<PriceUpdate>

fun watchPriceFeature(repo: PriceRepo) = streamFeature<WatchPriceQuery, PriceUpdate> {
    handle { request ->
        flow {
            val base = repo.findPrice(request.productId)
            var current = base.cents
            listOf(100, 250, -75).forEach { delta ->
                val newPrice = current + delta
                emit(PriceUpdate(request.productId, current, newPrice))
                current = newPrice
            }
        }
    }
}
