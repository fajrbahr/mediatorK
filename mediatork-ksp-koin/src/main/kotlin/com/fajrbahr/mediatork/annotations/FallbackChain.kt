package com.fajrbahr.mediatork.annotations

import kotlin.reflect.KClass

/**
 * Declares a fallback chain for a [com.fajrbahr.mediatork.handler.RequestHandler].
 *
 * Attach this annotation to the **primary** handler. The KSP processor will
 * automatically generate:
 * ```kotlin
 * registry register (PrimaryHandler() otherwise FallbackOne() otherwise FallbackTwo())
 * ```
 *
 * Rules:
 * - The annotated class itself is always tried first — do **not** include it in [fallbacks].
 * - [fallbacks] are tried in the order listed, left to right.
 * - Classes listed in [fallbacks] must **not** carry their own `@FallbackChain`.
 * - If a handler for the same request type exists without this annotation and
 *   is not listed as a fallback, the KSP processor will emit a compile-time error
 *   (ambiguous registration).
 *
 * Example:
 * ```kotlin
 * @FallbackChain(fallbacks = [CacheHandler::class, DbHandler::class])
 * class PrimaryHandler : RequestHandler<GetUser, User> { ... }
 *
 * class CacheHandler : RequestHandler<GetUser, User> { ... }
 * class DbHandler    : RequestHandler<GetUser, User> { ... }
 * ```
 *
 * @param fallbacks ordered list of fallback handler classes to try after the primary fails.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class FallbackChain(
    val fallbacks: Array<KClass<out Any>>,
)
