package sample.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestPreProcessor
import sample.context.CurrentUser
import sample.context.currentUser


class AuthPreProcessor : RequestPreProcessor {
    override suspend fun process(
        requestContext: RequestContext,
        request: Request<*>
    ) {
        // In a real app, you'd extract a token from the request or a thread-local
        val user = CurrentUser(
            id = "user-123",
            name = "Alice",
            email = "user@gmail.com",
            token = "JWT_",
            roles = listOf("admin", "user")
        )
        requestContext.currentUser = user
    }
}