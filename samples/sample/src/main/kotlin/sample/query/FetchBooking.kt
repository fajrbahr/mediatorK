package sample.query

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.RequestHandler

data class FetchUserQueryId(
    val userEmail: String,
    val bookingId: String
) : Request<String>

class FetchBookingHandler : RequestHandler<FetchUserQueryId, String> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: FetchUserQueryId
    ): String {
        return "Booking ${request.bookingId} found for ${request.userEmail}"
    }
}

class FetchUserHandlerRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +FetchBookingHandler()
        }
    }
}

