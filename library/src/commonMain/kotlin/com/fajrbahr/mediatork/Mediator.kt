package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.notification.Publisher

/**
 * Central mediator that combines request dispatching ([Sender]), stream dispatching
 * ([Streamer]), and notification broadcasting ([Publisher]) into a single entry point.
 *
 * Obtain an instance via [MediatorFactory.create]. The mediator is safe to share
 * across the application as a singleton; each [send] and [stream] call receives its
 * own isolated [RequestContext].
 *
 * @see Sender
 * @see Streamer
 * @see Publisher
 * @see MediatorFactory
 */
interface Mediator : Sender, Streamer, Publisher
