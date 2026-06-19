package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.MediatorFactory.create
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.notification.*
import com.fajrbahr.mediatork.handler.ThrowMissingRequestHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.StreamPipelineBehavior
import com.fajrbahr.mediatork.validator.ValidationBehavior


/**
 * Factory object for constructing a fully configured [com.fajrbahr.mediatork.api.Mediator] instance.
 *
 * Call [create] once at application startup, providing all registrars and
 * cross-cutting components, then share the returned [com.fajrbahr.mediatork.api.Mediator] as a singleton
 * throughout the application.
 *
 * @see com.fajrbahr.mediatork.api.MediatorRegistrar
 * @see PipelineBehavior
 * @see PipelineBehavior.Tag
 * @see com.fajrbahr.mediatork.notification.NotificationPublishStrategy
 */
object MediatorFactory {

    /**
     * Builds and returns a [com.fajrbahr.mediatork.api.Mediator] wired with the supplied components.
     *
     * Registration order:
     * 1. Each [com.fajrbahr.mediatork.api.MediatorRegistrar] in [registrars] is called to populate the [HandlerRegistry].
     * 2. If [verifyHandlers] is `true`, the registry is verified; a warning is printed to stdout
     *    for any request type whose handler is absent after registration.
     * 3. A [MediatorImpl] is constructed with the assembled registry and behaviors.
     *
     * @param registrars modules that contribute handlers to the registry.
     * @param pipelineBehaviors cross-cutting behaviors that wrap each request pipeline.
     *   Grouped by [PipelineBehavior.Tag] (PRE → DEFAULT → POST), then sorted by
     *   [PipelineBehavior.order] within each group; lower order = outermost within a phase.
     * @param notificationPublisher strategy for delivering notifications to their handlers.
     *   Defaults to [com.fajrbahr.mediatork.notification.ParallelNotificationPublisher].
     * @param verifyHandlers when `true` (the default), logs a warning for every registered request
     *   type that has no handler after all registrars have run.
     * @return a ready-to-use [com.fajrbahr.mediatork.api.Mediator] instance.
     */
    fun create(
        registrars: List<MediatorRegistrar> = emptyList(),
        pipelineBehaviors: List<PipelineBehavior> = emptyList(),
        streamPipelineBehaviors: List<StreamPipelineBehavior> = emptyList(),
        notificationPublisher: NotificationPublishStrategy = NotificationPublishStrategy.ParallelNotificationPublisher(),
        verifyHandlers: Boolean = true,
        missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
        missingRequestHandler: RequestHandler<Request<Any?>, Any?> = ThrowMissingRequestHandler(),
    ): Mediator {
        val registry = HandlerRegistry()

        registrars.forEach { it.register(registry) }

        if (verifyHandlers) {
            registry.verify{ typeName ->
                println("MEDIATOR WARNING: No handler registered for '$typeName'")
            }
        }

        return create(
            registry = registry,
            pipelineBehaviors = pipelineBehaviors,
            streamPipelineBehaviors = streamPipelineBehaviors,
            notificationPublisher = notificationPublisher,
            missingNotificationHandler = missingNotificationHandler,
            missingRequestHandler = missingRequestHandler,
        )
    }

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
        notificationPublisher: NotificationPublishStrategy = NotificationPublishStrategy.ParallelNotificationPublisher(),
        missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
        missingRequestHandler: RequestHandler<Request<Any?>, Any?> = ThrowMissingRequestHandler(),
    ): Mediator {
        val handlerValidators = registry.collectValidators()
        val allBehaviors = if (handlerValidators.isNotEmpty())
            listOf(ValidationBehavior(handlerValidators)) + pipelineBehaviors
        else
            pipelineBehaviors

        return MediatorImpl(
            registry = registry,
            pipelineBehaviors = allBehaviors,
            streamPipelineBehaviors = streamPipelineBehaviors,
            notificationPublisher = notificationPublisher,
            missingNotificationHandler = missingNotificationHandler,
            missingRequestHandler = missingRequestHandler,
        )
    }
}
