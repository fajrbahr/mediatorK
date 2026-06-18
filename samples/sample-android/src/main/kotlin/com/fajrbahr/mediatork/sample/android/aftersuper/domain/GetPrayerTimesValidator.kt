package com.fajrbahr.mediatork.sample.android.aftersuper.domain

import com.fajrbahr.mediatork.sample.android.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.validator.FieldValidator
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules
import kotlin.reflect.KClass

enum class CityField : FieldValidator { CITY, COUNTRY }

class CreateCityValidator : RequestValidator<GetPrayerTimesRequest> {
    override val requestClass: KClass<GetPrayerTimesRequest> = GetPrayerTimesRequest::class

    override fun validate(request: GetPrayerTimesRequest): ValidationResult = rules {
        ruleFor(CityField.CITY, request.city) { city ->
            check(city.isNotBlank()) { "City must not be blank" }
            check(city.length >= 2) { "City must be at least 2 characters" }
            check(city.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
                "City must contain only letters, spaces, or hyphens"
            }
        }
        ruleFor(CityField.COUNTRY, request.country) { country ->
            if (country.isNotEmpty()) {
                check(country.length >= 2) { "Country must be at least 2 characters" }
                check(country.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
                    "Country must contain only letters, spaces, or hyphens"
                }
            }
        }
    }
}

/** Kept for backward compatibility — delegates to [CreateCityValidator]. */
class GetPrayerTimesValidator : RequestValidator<GetPrayerTimesRequest> by CreateCityValidator()
