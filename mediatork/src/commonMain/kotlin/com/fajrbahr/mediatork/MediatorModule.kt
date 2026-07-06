package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.MediatorFactory.create
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.handler.ThrowMissingRequestHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ThrowMissingNotificationHandler
import com.fajrbahr.mediatork.validator.validationBehavior


/**
 * Factory object for constructing a fully configured [com.fajrbahr.mediatork.api.Mediator] instance.
 *
 * Call [create] once at application startup, providing all registrars and
 * cross-cutting components, then share the returned [com.fajrbahr.mediatork.api.Mediator] as a singleton
 * throughout the application.
 *
 * @see PipelineBehavior
 * @see com.fajrbahr.mediatork.notification.NotificationPublishStrategy
 */
object MediatorFactory {

    /**
     * Builds a [Mediator] from a pre-populated [registry].
     *
     * Prefer this overload in test helpers that need to hold a reference to the registry
     * so handlers can be added after construction.
     */
    fun create(
        registry: HandlerRegistry,
        pipelineBehaviors: List<PipelineBehavior> = emptyList(),
        streamPipelineBehaviors: List<StreamPipelineBehavior> = emptyList(),
        notificationPublisher: NotificationPublishStrategy =
            NotificationPublishStrategy.ParallelNotificationPublisher(),
        missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
        missingRequestHandler: RequestHandler<Request<Any?>, Any?> = ThrowMissingRequestHandler(),
    ): Mediator {
        val allBehaviors =
            listOf(validationBehavior(registry.anyValidators())) + pipelineBehaviors + registry.pipelineBehaviors
        val allStreamBehaviors = streamPipelineBehaviors + registry.streamPipelineBehaviors

        return MediatorImpl(
            registry = registry,
            pipelineBehaviors = allBehaviors,
            streamPipelineBehaviors = allStreamBehaviors,
            notificationPublisher = notificationPublisher,
            missingNotificationHandler = missingNotificationHandler,
            missingRequestHandler = missingRequestHandler,
        )
    }
}
