package com.fajrbahr.mediatork

/**
 * Contributes a set of handlers to the [HandlerRegistry] at mediator creation time.
 *
 * Implement this interface to group related handler registrations together — for
 * example, one registrar per feature module. All registrars are called once during
 * [MediatorFactory.create] before the mediator is built.
 *
 * ```kotlin
 * class OrderRegistrar : MediatorRegistrar {
 *     override fun register(registry: HandlerRegistry) {
 *         registry register PlaceOrderHandler()
 *         registry register CancelOrderHandler()
 *         registry registerNotification OrderShippedHandler()
 *     }
 * }
 * ```
 *
 * @see MediatorFactory
 * @see HandlerRegistry
 */
interface MediatorRegistrar {
    /**
     * Registers handlers into [registry].
     *
     * @param registry the shared registry to populate.
     */
    fun register(registry: HandlerRegistry)
}
