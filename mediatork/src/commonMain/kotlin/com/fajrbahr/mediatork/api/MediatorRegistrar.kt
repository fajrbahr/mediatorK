package com.fajrbahr.mediatork.api

import com.fajrbahr.mediatork.HandlerRegistry

/**
 * Contributes a set of handlers to the [com.fajrbahr.mediatork.HandlerRegistry] at mediator creation time.
 *
 * Implement this interface to group related handler registrations together — for
 * example, one registrar per feature module. All registrars are called once during
 * [com.fajrbahr.mediatork.MediatorFactory.create] before the mediator is built.
 *
 *
 * @see com.fajrbahr.mediatork.MediatorFactory
 * @see com.fajrbahr.mediatork.HandlerRegistry
 */
interface MediatorRegistrar {
    /**
     * Registers handlers into [registry].
     *
     * @param registry the shared registry to populate.
     */
    fun register(registry: HandlerRegistry)
}
