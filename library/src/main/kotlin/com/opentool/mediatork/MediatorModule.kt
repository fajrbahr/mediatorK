package com.opentool.mediatork.com.opentool.mediatork

object MediatorFactory {

    fun create(
        registrars: List<MediatorRegistrar> = emptyList(),
        pipelineBehaviors: List<PipelineBehavior> = emptyList(),
        preProcessors: List<RequestPreProcessor> = emptyList(),
        postProcessors: List<RequestPostProcessor> = emptyList(),
        notificationPublisher: NotificationPublisher = ParallelNotificationPublisher()
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


