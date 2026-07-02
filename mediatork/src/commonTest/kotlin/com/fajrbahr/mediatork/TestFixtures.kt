package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ParallelNotificationPublisher
import com.fajrbahr.mediatork.notification.ThrowMissingNotificationHandler

// ── Request / Notification types ─────────────────────────────────────────────

data class PingQuery(val value: String) : Request<String>
data class AddCommand(val a: Int, val b: Int) : Request<Int>
data class NoResultCommand(val id: String) : Request.Unit
data class EchoQuery(val text: String) : Request<String>
data class PingNotification(val message: String) : Notification
data class AlertNotification(val level: Int) : Notification

// ── Handlers ──────────────────────────────────────────────────────────────────

class PingHandler : RequestHandler<PingQuery, String> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) =
        "pong:${request.value}"
}

class AddHandler : RequestHandler<AddCommand, Int> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: AddCommand) =
        request.a + request.b
}

class NoResultHandler : RequestHandler<NoResultCommand, Unit> {
    var lastId: String? = null
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: NoResultCommand) {
        lastId = request.id
    }
}

class EchoHandler : RequestHandler<EchoQuery, String> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: EchoQuery) =
        request.text
}

class RecordingNotificationHandler : NotificationHandler<PingNotification> {
    val received = mutableListOf<String>()
    override suspend fun handle(notification: PingNotification) {
        received += notification.message
    }
}

class AlertNotificationHandler : NotificationHandler<AlertNotification> {
    val levels = mutableListOf<Int>()
    override suspend fun handle(notification: AlertNotification) {
        levels += notification.level
    }
}

// ── Builder helper ────────────────────────────────────────────────────────────

fun mediator(
    pipelineBehaviors: List<PipelineBehavior> = emptyList(),
    notificationPublisher: NotificationPublishStrategy = ParallelNotificationPublisher(),
    missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
    block: HandlerRegistry.() -> Unit,
): Mediator = MediatorFactory.create(
    registrars = listOf(object : MediatorRegistrar {
        override fun register(registry: HandlerRegistry) {
            registry.block()
        }
    }),
    pipelineBehaviors = pipelineBehaviors,
    notificationPublisher = notificationPublisher,
    missingNotificationHandler = missingNotificationHandler,
)
