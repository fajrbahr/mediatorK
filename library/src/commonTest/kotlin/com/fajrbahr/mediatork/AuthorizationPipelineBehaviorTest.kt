package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import com.fajrbahr.mediatork.pipeline.buildin.AuthenticatedRequest
import com.fajrbahr.mediatork.pipeline.buildin.AuthorizationPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.UnauthorizedException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private data class SecureQuery(val id: String) : Request<String>, AuthenticatedRequest
private data class PublicQuery(val id: String) : Request<String>

private class SecureHandler : RequestHandler<SecureQuery, String> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: SecureQuery) =
        "secure:${request.id}"
}

private class PublicHandler : RequestHandler<PublicQuery, String> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PublicQuery) =
        "public:${request.id}"
}

class AuthorizationPipelineBehaviorTest {

    @Test
    fun `allows request when authorize does not throw`() = runTest {
        val auth = AuthorizationPipelineBehavior(authorize = { _, _ -> /* allow */ })
        val m = mediator(pipelineBehaviors = listOf(auth)) { register(SecureHandler()) }
        assertEquals("secure:42", m.send(SecureQuery("42")))
    }

    @Test
    fun `blocks request when authorize throws`() = runTest {
        val auth = AuthorizationPipelineBehavior(authorize = { _, _ -> throw UnauthorizedException("no token") })
        val m = mediator(pipelineBehaviors = listOf(auth)) { register(SecureHandler()) }
        assertFailsWith<UnauthorizedException> { m.send(SecureQuery("1")) }
    }

    @Test
    fun `does not apply to non-AuthenticatedRequest`() = runTest {
        var authCalled = false
        val auth =
            AuthorizationPipelineBehavior(authorize = { _, _ -> authCalled = true; throw UnauthorizedException() })
        val m = mediator(pipelineBehaviors = listOf(auth)) { register(PublicHandler()) }
        m.send(PublicQuery("1"))
        assertEquals(false, authCalled)
    }

    @Test
    fun `reads token from RequestContext`() = runTest {
        val tokenBehavior = object : PipelineBehavior {
            override val order = -100
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                requestContext.put("token", "valid-token")
                return next(request)
            }
        }
        val auth = AuthorizationPipelineBehavior(authorize = { ctx, _ ->
            ctx.getMetaDate<String>("token") ?: throw UnauthorizedException()
        })
        val m = mediator(pipelineBehaviors = listOf(tokenBehavior, auth)) { register(SecureHandler()) }
        assertEquals("secure:1", m.send(SecureQuery("1")))
    }
}
