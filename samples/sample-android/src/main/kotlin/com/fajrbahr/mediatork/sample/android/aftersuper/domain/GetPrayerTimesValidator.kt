package com.fajrbahr.mediatork.sample.android.aftersuper.domain

import com.fajrbahr.mediatork.sample.android.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules
import kotlin.reflect.KClass

class CreateCityValidator : RequestValidator<GetPrayerTimesRequest> {
    override val requestClass: KClass<GetPrayerTimesRequest> = GetPrayerTimesRequest::class

    override fun validate(request: GetPrayerTimesRequest) = rules<String> {
        check(request.city.isNotBlank()) { "City must not be blank" }
        check(request.city.length >= 2) { "City must be at least 2 characters" }
        check(request.city.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
            "City must contain only letters, spaces, or hyphens"
        }
        if (request.country.isNotEmpty()) {
            check(request.country.length >= 2) { "Country must be at least 2 characters" }
            check(request.country.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
                "Country must contain only letters, spaces, or hyphens"
            }
        }
    }
}

/** Kept for backward compatibility — delegates to [CreateCityValidator]. */
class GetPrayerTimesValidator : RequestValidator<GetPrayerTimesRequest> by CreateCityValidator()
