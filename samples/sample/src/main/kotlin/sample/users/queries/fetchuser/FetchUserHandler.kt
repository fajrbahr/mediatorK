package sample.users.queries.fetchuser

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.handler.RequestHandler

class FetchUserHandler : RequestHandler<FetchUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: FetchUserQuery,
    ): User = User(name = "ali", email = "ali@gmail.com")
}

class UserRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            this register FetchUserHandler()
        }
    }
}
