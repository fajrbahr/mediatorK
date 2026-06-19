package sample.behaviors

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import sample.context.CurrentUser
import sample.context.currentUser

class AuthBehavior : PipelineBehavior {
    override val tag = PipelineBehavior.Tag.Pre

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val user = CurrentUser(
            id = "user-123",
            name = "Alice",
            email = "user@gmail.com",
            token = "JWT_",
            roles = listOf("admin", "user")
        )
        requestContext.currentUser = user
        return next(request)
    }
}
