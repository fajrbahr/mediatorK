package com.fajrbahr.mediatork

/**
 * Factory object for constructing a fully configured [Mediator] instance.
 *
 * Call [create] once at application startup, providing all registrars and
 * cross-cutting components, then share the returned [Mediator] as a singleton
 * throughout the application.
 *
 * @see MediatorRegistrar
 * @see PipelineBehavior
 * @see RequestPreProcessor
 * @see RequestPostProcessor
 * @see NotificationPublisher
 */
object MediatorFactory {

    /**
     * Builds and returns a [Mediator] wired with the supplied components.
     *
     * Registration order:
     * 1. Each [MediatorRegistrar] in [registrars] is called to populate the [HandlerRegistry].
     * 2. The registry is verified; a warning is printed to stdout for any request type whose
     *    handler is absent after registration.
     * 3. A [MediatorImpl] is constructed with the assembled registry and processors.
     *
     * @param registrars modules that contribute handlers to the registry.
     * @param pipelineBehaviors cross-cutting behaviors that wrap each request pipeline.
     *   Sorted by [PipelineBehavior.order] at dispatch time; lower order = outermost decorator.
     * @param preProcessors hooks that run before the handler; sorted by [RequestPreProcessor.order].
     * @param notificationPublisher strategy for delivering notifications to their handlers.
     *   Defaults to [ParallelNotificationPublisher].
     * @param postProcessors hooks that run after the handler; sorted by [RequestPostProcessor.order].
     * @return a ready-to-use [Mediator] instance.
     */
    fun create(
        registrars: List<MediatorRegistrar> = emptyList(),
        pipelineBehaviors: List<PipelineBehavior> = emptyList(),
        preProcessors: List<RequestPreProcessor> = emptyList(),
        notificationPublisher: NotificationPublisher = ParallelNotificationPublisher(),
        postProcessors: List<RequestPostProcessor> = emptyList()
    ): Mediator {
        val registry = HandlerRegistry()

        // Register all handlers
        registrars.forEach { it.register(registry) }

        // Verify handlers
        registry.verifyHandlers { typeName ->
            println("MEDIATOR WARNING: No handler registered for '$typeName'")
        }

        return MediatorImpl(
            registry = registry,
            pipelineBehaviors = pipelineBehaviors,
            preProcessors = preProcessors,
            postProcessors = postProcessors,
            notificationPublisher = notificationPublisher
        )
    }
}
