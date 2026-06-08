package com.fajrbahr.mediatork.validator

import kotlin.reflect.KClass

/**
 * Validates a request object and returns a [ValidationResult] describing any errors.
 *
 * Implement this interface to separate validation logic from handler logic.
 * Validators are typically invoked by a validation [com.fajrbahr.mediatork.PipelineBehavior]
 * that runs before the handler.
 *
 * @param TRequest the request type this validator handles.
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
     * Validates [request] and returns a [ValidationResult] containing any errors found.
     *
     * @param request the request to validate.
     * @return [ValidationResult.Success] if the request is valid, or a result containing
     *   one or more [ValidationError]s if not.
     */
    fun validate(request: TRequest): ValidationResult
}
