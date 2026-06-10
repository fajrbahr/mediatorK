package sample.query
import com.fajrbahr.mediatork.handler.*

import com.fajrbahr.mediatork.*


data class FetchUserQuery(
    val id: String,
    val amount: Double
) : Request<User>


data class User(
    val name: String,
    val email: String
)

class FetchUserHandler : RequestHandler<FetchUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: FetchUserQuery
    ): User {
        return User("ali", "ali@gmail.com")
    }
}

class UserRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            this register FetchUserHandler()
        }
    }
}

