package sample.bookings.queries.fetchbookings

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.RequestHandler

class FetchBookingsHandler : RequestHandler<FetchBookingsQuery, String> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: FetchBookingsQuery,
    ): String = "Booking ${request.bookingId} found for ${request.userEmail}"
}

class FetchBookingsRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +FetchBookingsHandler()
        }
    }
}
