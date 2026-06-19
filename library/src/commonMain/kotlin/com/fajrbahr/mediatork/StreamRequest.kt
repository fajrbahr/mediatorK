package com.fajrbahr.mediatork

import kotlinx.coroutines.flow.Flow

/**
 * A request that expects exactly one handler and returns a lazy [Flow] of results.
 *
 * Use [StreamRequest] when the response is a sequence of items produced over time —
 * large result sets, live feeds, cursor-based exports, or anything better consumed
 * incrementally than batched into a list.
 *
 * The handler produces a cold [Flow]; nothing executes until the caller collects it.
 * Dispatch via [IStreamRequest.stream] — a separate method from [Sender.send] so that
 * streaming and single-value requests are distinct at the call site.
 *
 *
 * @param T the type of each item emitted by the flow.
 * @see IStreamRequest
 * @see com.fajrbahr.mediatork.handler.StreamRequestHandler
 */
interface StreamRequest<out T>
