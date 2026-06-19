package com.fajrbahr.mediatork.sample.spring.aftersuper.domain

import com.fajrbahr.mediatork.sample.spring.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

class GetPrayerTimesValidator : RequestValidator<GetPrayerTimesRequest> {
    override fun validate(request: GetPrayerTimesRequest) = rules<String> {
        check(request.city.isNotBlank()) { "City must not be blank" }
        check(request.city.length >= 2) { "City must be at least 2 characters" }
        check(request.city.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
            "City must contain only letters"
        }
    }
}
