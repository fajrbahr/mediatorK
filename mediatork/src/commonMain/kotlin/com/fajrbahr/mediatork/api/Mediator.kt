package com.fajrbahr.mediatork.api

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.handler.Sender
import com.fajrbahr.mediatork.notification.Publisher

/**
 * Central mediator that combines request dispatching ([Sender]), stream dispatching
 * ([IStreamRequest]), and notification broadcasting ([Publisher]) into a single entry point.
 *
 * Obtain an instance via [com.fajrbahr.mediatork.MediatorFactory.create]. The mediator is safe to share
 * across the application as a singleton; each [send] and [stream] call receives its
 * own isolated [RequestContext].
 *
 * Use [registry] to register additional handlers at runtime.
 *
 * @see Sender
 * @see IStreamRequest
 * @see Publisher
 * @see com.fajrbahr.mediatork.MediatorFactory
 */
interface Mediator : Sender, IStreamRequest, Publisher {
    /** Access the handler registry to register/modify handlers at runtime. */
    val registry: HandlerRegistry
}
