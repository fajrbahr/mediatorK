package com.opentool.mediatork.com.opentool.mediatork.functional
//
//import org.koin.dsl.module
//
//val mediatorModule = module {
//    single { HandlerRegistry() }
//    single<NotificationPublisher> { ParallelNotificationPublisher }
//    single<Sender> { get<Mediator>() }
//    single<Publisher> { get<Mediator>() }
//    single<Mediator> {
//        val registry = get<HandlerRegistry>()
//        getAll<MediatorRegistrar>().forEach { it(registry) }
//
//        // Warn at startup if any registered request type is missing a handler.
//        // Crashes at first use otherwise-surfacing this at init time is much safer.
//        // Uses System.err so it shows in both Android logcat and iOS console without
//        // introducing a hard dependency on the logger module from mediator.
//        registry.verifyHandlers { typeName ->
//            println("MEDIATOR WARNING: No handler registered for '$typeName'")
//        }
//
//        MediatorImpl(
//            registry = registry,
//            pipelineBehaviors = getAll<PipelineBehavior>(),
//            preProcessors = getAll<RequestPreProcessor>(),
//            postProcessors = getAll<RequestPostProcessor>(),
//            notificationPublisher = get<NotificationPublisher>(),
//        )
//    }
//}
