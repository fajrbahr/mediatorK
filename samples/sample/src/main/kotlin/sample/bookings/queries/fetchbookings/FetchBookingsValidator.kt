package sample.bookings.queries.fetchbookings

import com.fajrbahr.mediatork.validator.FieldValidator
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rulesFailFast
import kotlin.reflect.KClass

class FetchBookingsValidator : RequestValidator<FetchBookingsQuery> {
    override val requestClass: KClass<FetchBookingsQuery> = FetchBookingsQuery::class

    override fun validate(request: FetchBookingsQuery): ValidationResult = rulesFailFast {
        ruleFor(FetchBookingsField.BookingId, request.bookingId) {
            check(it.isNotBlank()) { "Booking ID is required" }
            check(it.length > 3) { "Booking ID must be longer than 3 characters" }
            check(it.startsWith("bx")) { "Booking ID must start with bx" }
        }
        ruleFor(FetchBookingsField.UserEmail, request.userEmail) {
            check(EmailValidator.isValid(it)) { "A valid email address is required" }
        }
    }
}

sealed class FetchBookingsField : FieldValidator {
    object BookingId : FetchBookingsField()
    object UserEmail : FetchBookingsField()
}

private object EmailValidator {
    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    )

    fun isValid(input: String): Boolean {
        if (input.isBlank() || input.length > 254) return false
        return EMAIL_REGEX.matches(input.trim())
    }
}
