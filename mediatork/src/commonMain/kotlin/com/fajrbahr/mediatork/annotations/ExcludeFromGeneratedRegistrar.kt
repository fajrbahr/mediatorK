package com.fajrbahr.mediatork.annotations

/**
 * Excludes a handler class from KSP-generated registration.
 *
 * The `mediatork-ksp-koin` processor scans the compilation for concrete
 * [com.fajrbahr.mediatork.api.RequestHandler], [com.fajrbahr.mediatork.api.StreamRequestHandler]
 * and [com.fajrbahr.mediatork.api.NotificationHandler] implementations and registers
 * each of them in the generated `GeneratedMediatorRegistrar` and Koin module.
 *
 * Annotate a handler with this annotation to make the processor skip it, so the
 * handler can be constructed and registered manually instead — for example when it
 * needs constructor arguments that are not available in the DI container, must be
 * bound with a non-singleton scope, or should only be registered conditionally.
 *
 * ```kotlin
 * @ExcludeFromGeneratedRegistrar
 * class ManuallyWiredHandler(
 *     private val legacyDependency: LegacyService,
 * ) : RequestHandler<MyRequest, MyResult> { /* ... */ }
 * ```
 *
 * @see com.fajrbahr.mediatork.api.MediatorRegistrar
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExcludeFromGeneratedRegistrar
