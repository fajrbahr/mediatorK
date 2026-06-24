package sample.users.queries.fetchuser

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler

data class User(val id: String = "", val name: String = "", val email: String = "")

data class FetchUserQuery(val id: String, val amount: Double) : Request<User>

class FetchUserHandler : RequestHandler<FetchUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: FetchUserQuery,
    ): User = User(
        id = request.id,
        name = "Alice",
        email = "alice@example.com",
    )
}

class UserRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register FetchUserHandler()
    }
}
