package com.fajrbahr.mediatork.api

import com.fajrbahr.mediatork.validator.ValidationResult

interface Request<out TResponse> {
    fun validate(): ValidationResult = ValidationResult.Valid

    interface Unit : Request<kotlin.Unit>
}
