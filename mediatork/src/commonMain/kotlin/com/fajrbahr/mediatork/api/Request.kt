package com.fajrbahr.mediatork.api

import com.fajrbahr.mediatork.validator.ValidationResult

/**
 * A request that expects exactly one handler and a response.
 *
 * Use [Request] when the caller needs a result back, or needs to know the
 * operation succeeded or failed. There must be exactly one handler registered —
 * dispatching with no handler throws [com.fajrbahr.mediatork.MissingHandlerException].
 *
 * **Query** — read data, return it:
 * ```kotlin
 * data class GetTodoQuery(val id: String) : Request<Todo?>
 * ```
 *
 * **Command with result** — perform a side effect, return the outcome:
 * ```kotlin
 * data class AddTodoCommand(val id: String, val title: String) : Request<Todo>
 * ```
 *
 * **Command without result** — perform a side effect, no return value needed:
 * ```kotlin
 * data class DeleteTodoCommand(val id: String) : Request.Unit
 * ```
 *
 * **Inline validation** — override [validate] to add validation directly on the request:
 * ```kotlin
 * data class CreateCourseCommand(val title: String, val credits: Int) : Request<Int> {
 *     override fun validate() = rules<String> {
 *         check(title.length in 3..50) { "Title must be between 3 and 50 characters" }
 *         check(credits in 0..5) { "Credits must be between 0 and 5" }
 *     }
 * }
 * ```
 *
 * @see Notification for broadcasting events to zero-or-many handlers with no response.
 */
interface Request<out TResponse> {

    /**
     * Override to add validation rules directly on this request.
     * Defaults to [ValidationResult.Valid] (no validation).
     * Runs automatically before the handler when [com.fajrbahr.mediatork.validator.ValidationBehavior] is active.
     */
    fun validate(): ValidationResult = ValidationResult.Valid

    /** Convenience marker for commands that return no value. Use instead of `Request<Unit>`. */
    interface Unit : Request<kotlin.Unit>
}
