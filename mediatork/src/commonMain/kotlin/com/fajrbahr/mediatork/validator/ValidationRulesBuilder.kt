package com.fajrbahr.mediatork.validator

class ValidationException(val errors: List<*>, cause: Throwable? = null) :
    Exception(errors.joinToString("; ") { it.toString() }, cause) {
    constructor(message: String, cause: Throwable? = null) : this(listOf(message), cause)
}

/**
 * Runs all checks in [block] and returns [ValidationResult.Invalid] with the full error list
 * if any fail, or [ValidationResult.Valid] if all pass.
 */
fun <T : Any> rules(block: RulesBuilder<T>.() -> Unit): ValidationResult =
    RulesBuilder<T>().apply(block).toResult()

/**
 * Runs checks in [block] and returns [ValidationResult.Invalid] with the **first** failing error,
 * or [ValidationResult.Valid] if all pass. Remaining checks are skipped after the first failure.
 */
fun <T : Any> rulesFailFast(block: FailFastRulesBuilder<T>.() -> Unit): ValidationResult {
    val builder = FailFastRulesBuilder<T>()
    return try {
        builder.block()
        ValidationResult.Valid
    } catch (_: FailFastRulesBuilder.FailFastSignal) {
        ValidationResult.Invalid(listOf(builder.firstError!!))
    }
}

/**
 * Collects all validation errors and returns them together.
 *
 * Call [check] or [require] for each rule — all checks run regardless of earlier failures.
 * After the block completes, [toResult] returns [ValidationResult.Valid] if the error list
 * is empty, or [ValidationResult.Invalid] with every collected error otherwise.
 *
 * Instantiated by [rules].
 */
class RulesBuilder<T : Any> {
    private val errors = mutableListOf<T>()

    /**
     * Adds [message] to the error list when [condition] is `false`. All subsequent checks still run.
     */
    fun check(condition: Boolean, message: () -> T) {
        if (!condition) errors += message()
    }

    /**
     * Alias for [check]. Adds [message] to the error list when [condition] is `false`.
     */
    fun require(condition: Boolean, message: () -> T) {
        if (!condition) errors += message()
    }

    internal fun toResult(): ValidationResult =
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors.toList())
}

/**
 * Stops at the first failing check and returns only that error.
 *
 * Call [check] or [require] for each rule — execution stops at the first failure.
 * After the block completes, the result is [ValidationResult.Valid] if no check failed,
 * or [ValidationResult.Invalid] with the single first error otherwise.
 *
 * Instantiated by [rulesFailFast].
 */
class FailFastRulesBuilder<T : Any> {
    internal var firstError: T? = null

    /** Internal control-flow signal — not an actual exception type. */
    internal class FailFastSignal : Throwable()

    /**
     * Records [message] as the first error and halts the block when [condition] is `false`.
     */
    fun check(condition: Boolean, message: () -> T) {
        if (!condition) {
            firstError = message(); throw FailFastSignal()
        }
    }

    /**
     * Alias for [check]. Records [message] as the first error and halts when [condition] is `false`.
     */
    fun require(condition: Boolean, message: () -> T) {
        if (!condition) {
            firstError = message(); throw FailFastSignal()
        }
    }
}
