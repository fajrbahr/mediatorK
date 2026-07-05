package dsl.meditor

import dsl.meditor.price.RawPrice

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

val repo = object : PriceRepo {
    override fun findPrice(productId: String) = RawPrice(productId, cents = 2999)
}

val pushService = object : PushService {
    override fun send(orderId: String, phone: String) {
        println("  [PUSH] Sent to $phone for order $orderId")
    }
}

val inAppService = object : InAppService {
    override fun notify(orderId: String) {
        println("  [IN-APP] Notification sent for order $orderId")
    }
}