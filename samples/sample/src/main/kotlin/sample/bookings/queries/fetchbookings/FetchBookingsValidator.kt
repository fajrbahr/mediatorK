package sample.bookings.queries.fetchbookings

import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.validator.rules

class FetchBookingsValidator : RequestValidator<FetchBookingsQuery> {
    override fun validate(request: FetchBookingsQuery) = rules<String> {
        check(request.bookingId.isNotBlank()) { "Booking ID is required" }
        check(request.bookingId.length > 3) { "Booking ID must be longer than 3 characters" }
        check(request.bookingId.startsWith("bx")) { "Booking ID must start with bx" }
        check(EmailValidator.isValid(request.userEmail)) { "A valid email address is required" }
    }
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
