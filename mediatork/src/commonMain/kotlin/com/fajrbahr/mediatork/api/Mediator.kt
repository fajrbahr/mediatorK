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
 * Register additional handlers at runtime via the [com.fajrbahr.mediatork.add] DSL function.
 *
 * @see Sender
 * @see IStreamRequest
 * @see Publisher
 * @see com.fajrbahr.mediatork.MediatorFactory
 * @see com.fajrbahr.mediatork.add
 */
interface Mediator : Sender, IStreamRequest, Publisher
