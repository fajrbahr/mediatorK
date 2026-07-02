package dsl.meditor

import dsl.meditor.products.InAppService
import dsl.meditor.products.PriceRepo
import dsl.meditor.products.PushService
import dsl.meditor.products.RawPrice

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