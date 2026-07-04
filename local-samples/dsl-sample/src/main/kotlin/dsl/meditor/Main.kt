package dsl.meditor

import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import dsl.meditor.behaviors.LocaleBehavior
import dsl.meditor.behaviors.MeasurePipelineBehaviour
import dsl.meditor.products.GetPriceQuery
import dsl.meditor.products.ProductRegistrar
import dsl.meditor.products.WatchPriceQuery
import kotlinx.coroutines.flow.collect
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
    // ── Mapped Feature with multiple validators + bundled behavior ──────────
    println("=== Mapped Feature: GetPrice ===")
    val price = mediator.send(GetPriceQuery(productId = "PROD-1"))
    println("Price: $price")

    // ── Stream Feature ─────────────────────────────────────────────────────
    println("\n=== Stream Feature: WatchPriceUpdates ===")
    mediator.stream(WatchPriceQuery(productId = "PROD-1")).collect { update ->
        println("  ${update.productId}: ${update.oldCents}c -> ${update.newCents}c")
    }
}
