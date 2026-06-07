package sample.query

import com.fajrbahr.mediatork.*

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
        return "sddsd"
    }
}

class FetchUserHandlerRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +FetchBookingHandler()
        }
    }
}

