package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.handler.*
import com.fajrbahr.mediatork.pipeline.*

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.TimeoutCancellationException

class TimeoutPipelineBehaviorTest {

    @Test
    fun `completes normally when handler finishes within timeout`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(TimeoutPipelineBehavior(timeoutMillis = 5_000))) {
            register(PingHandler())
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `throws TimeoutCancellationException when handler exceeds timeout`() = runTest {
        val slowHandler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                delay(10_000)
                return "too late"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(TimeoutPipelineBehavior(timeoutMillis = 100))) {
            register(slowHandler)
        }
        assertFailsWith<TimeoutCancellationException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `requires timeoutMillis greater than zero`() {
        assertFailsWith<IllegalArgumentException> {
            TimeoutPipelineBehavior(timeoutMillis = 0)
        }
    }
}
