package sample.validation

import com.fajrbahr.mediatork.validator.FieldV
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rulesFailFast
import sample.query.FetchUserQueryId
import kotlin.reflect.KClass

class FetchBookingsByEmailQueryValidator : RequestValidator<FetchUserQueryId> {
    override val requestClass: KClass<FetchUserQueryId> = FetchUserQueryId::class

    override fun validate(request: FetchUserQueryId): ValidationResult = rulesFailFast {
        ruleFor(FetchBookingsByEmailQueryValidatorField.BookingId, request.bookingId) {
            check(it.isNotBlank()) { "Booking ID is required" }
            check(it.length > 3) { "Booking ID must be longer than 3 characters" }
            check(it.startsWith("bx")) { "Booking ID must start with bx" }
        }
        ruleFor(FetchBookingsByEmailQueryValidatorField.UserEmail, request.userEmail) {
            check(EmailValidator.isValid(it)) { "A valid email address is required" }
        }
    }
}

sealed class FetchBookingsByEmailQueryValidatorField : FieldV {
    object BookingId : FetchBookingsByEmailQueryValidatorField()
    object UserEmail : FetchBookingsByEmailQueryValidatorField()
}
