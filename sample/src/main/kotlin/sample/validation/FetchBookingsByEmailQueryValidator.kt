package sample.validation

import com.fajrbahr.mediatork.validator.FieldV
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules
import com.fajrbahr.mediatork.validator.rulesFailFast
import sample.query.FetchUserQueryId
import kotlin.reflect.KClass

class FetchBookingsByEmailQueryValidator : RequestValidator<FetchUserQueryId> {
    override val requestClass: KClass<FetchUserQueryId> = FetchUserQueryId::class

    override fun validate(request: FetchUserQueryId): ValidationResult = rules {
        check(request.bookingId.isNotBlank()) { "Booking ID is required" }

        ruleFor(FetchBookingsByEmailQueryValidatorField.BookingId, request.bookingId) {
            check(it.isNotBlank()) { "Booking ID is required" }
            check(it.length > 3) { "Booking ID must be longer than 3 characters" }  // clearer message
            check(it.startsWith("bx")) { "Booking ID must be start with bx " }  // clearer message
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
class FetchBookingsByEmailQueryValidatorF : RequestValidator<FetchUserQueryId> {
    override val requestClass: KClass<FetchUserQueryId> = FetchUserQueryId::class

    override fun validate(request: FetchUserQueryId): ValidationResult = rulesFailFast {
   //     check(request.bookingId.isNotBlank()) { "Booking ID is required" }

        ruleFor(FetchBookingsByEmailQueryValidatorField.BookingId, request.bookingId) {
            check(it.isNotBlank()) { "Booking ID is required" }
        }
        ruleFor(FetchBookingsByEmailQueryValidatorField.UserEmail, request.userEmail) {
            check(EmailValidator.isValid(it)) { "A valid email address is required" }
        }
    }
}