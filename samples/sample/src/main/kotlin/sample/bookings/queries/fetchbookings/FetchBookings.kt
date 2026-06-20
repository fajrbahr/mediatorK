package sample.bookings.queries.fetchbookings

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

data class Booking(
    val bookingId: String,
    val userEmail: String,
    val status: String,
)

data class FetchBookingsQuery(
    val userEmail: String,
    val bookingId: String,
) : Request<Booking>

class FetchBookingsValidator : RequestValidator<FetchBookingsQuery> {
    override fun validate(request: FetchBookingsQuery): ValidationResult = rules<String> {
        check(request.userEmail.contains("@") && request.userEmail.contains(".")) {
            "Invalid email address: '${request.userEmail}'"
        }
        check(request.bookingId.startsWith("bx_booking#")) {
            "Booking ID must start with 'bx_booking#', got '${request.bookingId}'"
        }
    }
}

class FetchBookingsHandler : RequestHandler<FetchBookingsQuery, Booking> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: FetchBookingsQuery,
    ): Booking = Booking(
        bookingId = request.bookingId,
        userEmail = request.userEmail,
        status = "CONFIRMED",
    )
}

class FetchBookingsRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register FetchBookingsHandler()
        registry.registerValidator(FetchBookingsValidator())
    }
}
