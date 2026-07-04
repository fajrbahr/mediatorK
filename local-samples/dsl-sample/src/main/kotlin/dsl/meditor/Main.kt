package dsl.meditor

import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import dsl.meditor.behaviors.LocaleBehavior
import dsl.meditor.behaviors.MeasurePipelineBehaviour
import dsl.meditor.products.GetPriceQuery
import dsl.meditor.products.ProductRegistrar
import kotlinx.coroutines.runBlocking


private val mediator = mediatorK {

    // ── Register all product features via registrar ─────────────────────────
    registrars(ProductRegistrar(repo, pushService, inAppService))

    // ── Pipeline behaviors ──────────────────────────────────────────────────
    behaviors(
        LocaleBehavior(),
        MeasurePipelineBehaviour(),
        LoggingPipelineBehavior(),
    )

    // ── Configuration ───────────────────────────────────────────────────────
    notificationPublisher = NotificationPublishStrategy.SequentialNotificationPublisher()
    missingNotificationHandler = SilentMissingNotificationHandler()
}

fun main(): Unit = runBlocking {
    // ── 11. Mapped Feature DSL ──────────────────────────────────────────────
    println("=== Mapped Feature: GetPrice ===")
    val price = mediator.send(GetPriceQuery(productId = "PROD-1"))
    println("Price: $price")
}
