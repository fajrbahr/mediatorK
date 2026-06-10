package com.fajrbahr.mediatork.test
import com.fajrbahr.mediatork.handler.*

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.RequestHandler
import io.github.classgraph.ClassGraph
import kotlin.test.assertTrue

object MediatorTestUtils {

    /**
     * Asserts that every concrete [RequestHandler] found on the classpath is registered
     * via the provided [registrars].
     *
     * Use this in a unit test to catch handlers that were created but never wired into a
     * [MediatorRegistrar] — turning a silent runtime crash into a clear test failure.
     *
     * ## Usage
     *
     * ```kotlin
     * @Test
     * fun `all handlers are registered`() {
     *     MediatorTestUtils.assertAllHandlersRegistered(
     *         registrars = listOf(OrderRegistrar(), UserRegistrar()),
     *     )
     * }
     * ```
     *
     * Scan a specific set of packages to avoid false positives from third-party handlers
     * on the classpath:
     *
     * ```kotlin
     * MediatorTestUtils.assertAllHandlersRegistered(
     *     registrars = listOf(OrderRegistrar(), UserRegistrar()),
     *     packages = listOf("com.myapp.order", "com.myapp.user"),
     * )
     * ```
     *
     * ## Failure message
     *
     * When a handler is missing, the test fails with a message like:
     * ```
     * Unregistered handlers found:
     *   - CreateOrderHandler handles CreateOrderCommand — not registered
     * ```
     *
     * @param registrars the same registrars passed to [com.fajrbahr.mediatork.MediatorFactory.create].
     * @param packages packages to scan for [RequestHandler] implementations.
     *   Defaults to the entire classpath. Narrow this when third-party libraries on the
     *   classpath also implement [RequestHandler] and would cause false positives.
     */
    fun assertAllHandlersRegistered(
        registrars: List<MediatorRegistrar>,
        packages: List<String> = emptyList(),
    ) {
        val graph = ClassGraph().enableClassInfo()
        if (packages.isNotEmpty()) graph.acceptPackages(*packages.toTypedArray())

        val handlerClasses = graph.scan().use { result ->
            result.getClassesImplementing(RequestHandler::class.java.name)
                .filter { !it.isAbstract && !it.isInterface && !it.isAnonymousInnerClass }
                .map { it.loadClass() }
        }

        if (handlerClasses.isEmpty()) return

        val registry = HandlerRegistry().also { r -> registrars.forEach { it.register(r) } }
        val registeredRequestTypes = registry.registeredRequestTypes()

        val missing = handlerClasses.mapNotNull { handlerClass ->
            val requestType = handlerClass.genericInterfaces
                .filterIsInstance<java.lang.reflect.ParameterizedType>()
                .firstOrNull { it.rawType == RequestHandler::class.java }
                ?.actualTypeArguments
                ?.firstOrNull() as? Class<*>

            if (requestType != null && requestType.kotlin !in registeredRequestTypes) {
                "${handlerClass.simpleName} handles ${requestType.simpleName} — not registered"
            } else null
        }

        assertTrue(missing.isEmpty(), "Unregistered handlers found:\n${missing.joinToString("\n") { "  - $it" }}")
    }
}
