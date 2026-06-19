package com.fajrbahr.mediatork

/**
 * A request that expects exactly one handler and a response.
 *
 * Use [Request] when the caller needs a result back, or needs to know the
 * operation succeeded or failed. There must be exactly one handler registered —
 * dispatching with no handler throws [com.fajrbahr.mediatork.MissingHandlerException].
 *
 * **Query**-read data, return it:
 *
 * **Command with result**-perform a side effect, return outcome:
 *
 * **Command without result**-perform a side effect, no return value needed:
 *
 * @see com.fajrbahr.mediatork.notification.Notification for broadcasting events to zero-or-many handlers with no response.
 */
interface Request<out TResponse> {
    /** Convenience marker for commands that return no value. Use instead of `Request<Unit>`. */
    interface Unit : Request<kotlin.Unit>
}
