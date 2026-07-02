package com.fajrbahr.mediatork.validator

/**
 * Runs all checks in [block] and returns [ValidationResult.Invalid] with the full error list
 * if any fail, or [ValidationResult.Valid] if all pass. Warnings are always collected and
 * included in the result regardless of whether errors are present.
 */
fun <T : Any> rules(block: RulesBuilder<T>.() -> Unit): ValidationResult =
    RulesBuilder<T>().apply(block).toResult()

/**
 * Runs checks in [block] and returns [ValidationResult.Invalid] with the **first** failing error,
 * or [ValidationResult.Valid] if all pass. Remaining checks are skipped after the first failure.
 * Warnings are always collected — [warn] never triggers fail-fast.
 */
fun <T : Any> rulesFailFast(block: FailFastRulesBuilder<T>.() -> Unit): ValidationResult {
    val builder = FailFastRulesBuilder<T>()
    return try {
        builder.block()
        builder.validResult()
    } catch (_: FailFastRulesBuilder.FailFastSignal) {
        ValidationResult.Invalid(listOf(builder.firstError!!), builder.warnings.toList())
    }
}

/**
 * Collects all validation errors and warnings, returning them together.
 *
 * Call [check] or [require] for each error rule, and [warn] for non-blocking warnings —
 * all checks run regardless of earlier failures.
 * After the block completes, [toResult] returns:
 * - [ValidationResult.Valid] if no errors and no warnings,
 * - [ValidationResult.ValidWithWarnings] if only warnings,
 * - [ValidationResult.Invalid] (with any warnings) if errors are present.
 *
 * Instantiated by [rules].
 */
class RulesBuilder<T : Any> {
    private val errors = mutableListOf<T>()
    private val warnings = mutableListOf<T>()

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

    /**
     * Adds [message] to the warning list when [condition] is `true`.
     * Warnings are informational and never cause validation to fail.
     */
    fun warn(condition: Boolean, message: () -> T) {
        if (condition) warnings += message()
    }

    internal fun toResult(): ValidationResult = when {
        errors.isNotEmpty() -> ValidationResult.Invalid(errors.toList(), warnings.toList())
        warnings.isNotEmpty() -> ValidationResult.ValidWithWarnings(warnings.toList())
        else -> ValidationResult.Valid
    }
}

/**
 * Stops at the first failing error check and returns only that error.
 *
 * Call [check] or [require] for each error rule — execution stops at the first failure.
 * Call [warn] for non-blocking warnings — warnings never trigger fail-fast and are always collected.
 * After the block completes, the result is [ValidationResult.Valid] if no check failed,
 * or [ValidationResult.Invalid] with the single first error otherwise.
 * Warnings are included in either case.
 *
 * Instantiated by [rulesFailFast].
 */
class FailFastRulesBuilder<T : Any> {
    internal var firstError: T? = null
    internal val warnings = mutableListOf<T>()

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

    /**
     * Adds [message] to the warning list when [condition] is `true`.
     * Warnings are informational and never trigger fail-fast.
     */
    fun warn(condition: Boolean, message: () -> T) {
        if (condition) warnings += message()
    }

    internal fun validResult(): ValidationResult =
        if (warnings.isEmpty()) ValidationResult.Valid
        else ValidationResult.ValidWithWarnings(warnings.toList())
}
