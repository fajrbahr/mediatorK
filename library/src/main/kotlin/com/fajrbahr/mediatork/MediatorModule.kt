package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.MediatorFactory.create
import com.fajrbahr.mediatork.notification.*
import com.fajrbahr.mediatork.pipeline.PipelineBehavior


/**
 * Factory object for constructing a fully configured [Mediator] instance.
 *
 * Call [create] once at application startup, providing all registrars and
 * cross-cutting components, then share the returned [Mediator] as a singleton
 * throughout the application.
 *
 * @see MediatorRegistrar
 * @see com.fajrbahr.mediatork.pipeline.PipelineBehavior
 * @see RequestPreProcessor
 * @see RequestPostProcessor
 * @see com.fajrbahr.mediatork.notification.NotificationPublisher
 */
object MediatorFactory {

    /**
     * Builds and returns a [Mediator] wired with the supplied components.
     *
     * Registration order:
     * 1. Each [MediatorRegistrar] in [registrars] is called to populate the [HandlerRegistry].
     * 2. If [verifyHandlers] is `true`, the registry is verified; a warning is printed to stdout
     *    for any request type whose handler is absent after registration.
     * 3. A [MediatorImpl] is constructed with the assembled registry and processors.
     *
     * @param registrars modules that contribute handlers to the registry.
     * @param pipelineBehaviors cross-cutting behaviors that wrap each request pipeline.
     *   Sorted by [com.fajrbahr.mediatork.pipeline.PipelineBehavior.order] at dispatch time; lower order = outermost decorator.
     * @param preProcessors hooks that run before the handler; sorted by [RequestPreProcessor.order].
     * @param notificationPublisher strategy for delivering notifications to their handlers.
     *   Defaults to [com.fajrbahr.mediatork.notification.ParallelNotificationPublisher].
     * @param postProcessors hooks that run after the handler; sorted by [RequestPostProcessor.order].
     * @param verifyHandlers when `true` (the default), logs a warning for every registered request
     *   type that has no handler after all registrars have run. Set to `false` to suppress these
     *   warnings (e.g. in test setups where partial registration is intentional).
     * @return a ready-to-use [Mediator] instance.
     */
    fun create(
        registrars: List<MediatorRegistrar> = emptyList(),
        pipelineBehaviors: List<PipelineBehavior> = emptyList(),
        preProcessors: List<RequestPreProcessor> = emptyList(),
        notificationPublisher: NotificationPublisher = ParallelNotificationPublisher(),
        postProcessors: List<RequestPostProcessor> = emptyList(),
        verifyHandlers: Boolean = true,
        missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
    ): Mediator {
        val registry = HandlerRegistry()

        registrars.forEach { it.register(registry) }

        if (verifyHandlers) {
            registry.verifyHandlers { typeName ->
                println("MEDIATOR WARNING: No handler registered for '$typeName'")
            }
        }

        return create(
            registry = registry,
            pipelineBehaviors = pipelineBehaviors,
            preProcessors = preProcessors,
            postProcessors = postProcessors,
            notificationPublisher = notificationPublisher,
            missingNotificationHandler = missingNotificationHandler,
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
        preProcessors: List<RequestPreProcessor> = emptyList(),
        notificationPublisher: NotificationPublisher = ParallelNotificationPublisher(),
        postProcessors: List<RequestPostProcessor> = emptyList(),
        missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
    ): Mediator = MediatorImpl(
        registry = registry,
        pipelineBehaviors = pipelineBehaviors,
        preProcessors = preProcessors,
        postProcessors = postProcessors,
        notificationPublisher = notificationPublisher,
        missingNotificationHandler = missingNotificationHandler,
    )
}
