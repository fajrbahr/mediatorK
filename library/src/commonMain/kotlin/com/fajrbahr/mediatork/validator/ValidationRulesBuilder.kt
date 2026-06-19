package com.fajrbahr.mediatork.validator

/**
 * Runs all checks in [block] and returns [ValidationResult.Invalid] with the full error list
 * if any fail, or [ValidationResult.Valid] if all pass.
 *
 */
fun <T : Any> rules(block: RulesBuilder<T>.() -> Unit): ValidationResult =
    RulesBuilder<T>().apply(block).toResult()

/**
 * Runs checks in [block] and returns [ValidationResult.Invalid] with the **first** failing error,
 * or [ValidationResult.Valid] if all pass. Remaining checks are skipped after the first failure.
 *
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

class RulesBuilder<T : Any> {
    private val errors = mutableListOf<T>()

    fun check(condition: Boolean, message: () -> T) {
        if (!condition) errors += message()
    }

    fun require(condition: Boolean, message: () -> T) {
        if (!condition) errors += message()
    }

    internal fun toResult(): ValidationResult =
        if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors.toList())
}

class FailFastRulesBuilder<T : Any> {
    internal var firstError: T? = null

    internal class FailFastSignal : Throwable()

    fun check(condition: Boolean, message: () -> T) {
        if (!condition) {
            firstError = message(); throw FailFastSignal()
        }
    }

    fun require(condition: Boolean, message: () -> T) {
        if (!condition) {
            firstError = message(); throw FailFastSignal()
        }
    }
}
