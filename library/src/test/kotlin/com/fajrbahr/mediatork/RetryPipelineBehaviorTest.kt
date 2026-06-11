package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.pipeline.RetryPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryPipelineBehaviorTest {

    private fun flakyHandler(failTimes: Int): RequestHandler<PingQuery, String> {
        var calls = 0
        return object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                calls++
                if (calls <= failTimes) throw RuntimeException("attempt $calls failed")
                return "pong:${request.value}"
            }
        }
    }

    @Test
    fun `succeeds on first attempt when no failure`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(RetryPipelineBehavior(maxRetries = 3))) {
            register(PingHandler())
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `retries and succeeds after transient failures`() = runTest {
        val handler = flakyHandler(failTimes = 2)
        val m = mediator(pipelineBehaviors = listOf(RetryPipelineBehavior(maxRetries = 3))) {
            register(handler)
        }
        assertEquals("pong:hi", m.send(PingQuery("hi")))
    }

    @Test
    fun `rethrows after exhausting all retries`() = runTest {
        val handler = flakyHandler(failTimes = 5)
        val m = mediator(pipelineBehaviors = listOf(RetryPipelineBehavior(maxRetries = 2))) {
            register(handler)
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
    }

    private class NonRetryableException(message: String) : Exception(message)

    @Test
    fun `does not retry when retryOn returns false`() = runTest {
        var attempts = 0
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String {
                attempts++
                throw NonRetryableException("non-retryable")
            }
        }
        val behavior = RetryPipelineBehavior(
            maxRetries = 3,
            retryOn = { it is RuntimeException },
        )
        val m = mediator(pipelineBehaviors = listOf(behavior)) { register(handler) }
        assertFailsWith<NonRetryableException> { m.send(PingQuery("x")) }
        assertEquals(1, attempts)
    }

    @Test
    fun `maxRetries=0 means exactly one attempt`() = runTest {
        val handler = flakyHandler(failTimes = 1)
        val m = mediator(pipelineBehaviors = listOf(RetryPipelineBehavior(maxRetries = 0))) {
            register(handler)
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
    }
}
