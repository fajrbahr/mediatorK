package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.notification.*

/**
 * Central mediator that combines request dispatching ([Sender]) and notification
 * broadcasting ([Publisher]) into a single entry point.
 *
 * Obtain an instance via [MediatorFactory.create]. The mediator is safe to share
 * across the application as a singleton; each [send] call receives its own
 * isolated [RequestContext].
 *
 * @see Sender
 * @see Publisher
 * @see MediatorFactory
 */
interface Mediator : Sender, Publisher
