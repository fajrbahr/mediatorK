package dsl.meditor

import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import dsl.meditor.behaviors.LocaleBehavior
import dsl.meditor.behaviors.MeasurePipelineBehaviour
import dsl.meditor.products.GetPriceQuery
import dsl.meditor.products.getPriceFeature
import kotlinx.coroutines.runBlocking


private val mediator = mediatorK {

    // ── Mapped Feature DSL — handler with result mapping via mapper() ───────
    +getPriceFeature(repo, pushService, inAppService)

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
