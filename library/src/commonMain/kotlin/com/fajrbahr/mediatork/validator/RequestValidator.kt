package com.fajrbahr.mediatork.validator

import kotlin.reflect.KClass

/**
 * Validates a request object and returns a [ValidationResult] describing any errors.
 *
 * Implement this interface to separate validation logic from handler logic. Declare a
 * [scope] to indicate when the validator should run:
 *
 * - [ValidationScope.REQUEST] validators (default) are run automatically by [ValidationBehavior]
 *   before the handler. Use them for field-format and type checks.
 * - [ValidationScope.DOMAIN] validators must be called explicitly inside a handler after the
 *   domain aggregate has been loaded. Use them for business-rule checks.
 * - [ValidationScope.PERSISTENCE] validators must be called explicitly inside a handler just
 *   before a database write. Use them for uniqueness and foreign-key checks.
 *
 * @param TRequest the request type this validator handles.
 * @see ValidationScope
 * @see ValidationResult
 * @see ValidationRulesBuilder
 */
interface RequestValidator<TRequest : Any> {
    /**
     * The [KClass] of the request type this validator is responsible for.
     * Used by a validation pipeline behavior to look up the correct validator.
     */
    val requestClass: KClass<TRequest>

    /**
     * The lifecycle stage at which this validator should run.
     * Defaults to [ValidationScope.REQUEST] so existing validators are unaffected.
     */
    val scope: ValidationScope get() = ValidationScope.REQUEST

    /**
     * Validates [request] and returns a [ValidationResult] containing any errors found.
     *
     * @param request the request to validate.
     * @return [ValidationResult.Success] if the request is valid, or a result containing
     *   one or more [ValidationError]s if not.
     */
    fun validate(request: TRequest): ValidationResult
}
