package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.timeoutPipelineBehavior
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeoutPipelineBehaviorTest {

    @Test
    fun `completes normally when handler finishes within timeout`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(timeoutPipelineBehavior(timeoutMillis = 5_000))) {
            add(PingHandler())
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `throws TimeoutCancellationException when handler exceeds timeout`() = runTest {
        val slowHandler = RequestHandler<PingQuery, String> { mediator, requestContext, request ->
            delay(10_000.milliseconds)
            "too late"
        }
        val m = mediator(pipelineBehaviors = listOf(timeoutPipelineBehavior(timeoutMillis = 100))) {
            add(slowHandler)
        }
        assertFailsWith<TimeoutCancellationException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `requires timeoutMillis greater than zero`() {
        assertFailsWith<IllegalArgumentException> {
            timeoutPipelineBehavior(timeoutMillis = 0)
        }
    }

    @Test
    fun `negative timeoutMillis throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            timeoutPipelineBehavior(timeoutMillis = -1)
        }
    }

    @Test
    fun `default order is 0`() {
        assertEquals(0, timeoutPipelineBehavior(timeoutMillis = 1_000).order)
    }

    @Test
    fun `custom order value is reflected on instance`() {
        assertEquals(-50, timeoutPipelineBehavior(timeoutMillis = 1_000, order = -50).order)
    }

    @Test
    fun `result is passed through unchanged when handler completes in time`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(timeoutPipelineBehavior(timeoutMillis = 5_000))) {
            add(AddHandler())
        }
        assertEquals(9, m.send(AddCommand(4, 5)))
    }


    @Test
    fun `result passes through for AddCommand within timeout`() = runTest {
        val m = mediator(pipelineBehaviors = listOf(timeoutPipelineBehavior(timeoutMillis = 5_000))) {
            add(AddHandler())
        }
        assertEquals(15, m.send(AddCommand(7, 8)))
    }

    @Test
    fun `multiple requests all complete when within timeout`() = runTest {
        val behavior = timeoutPipelineBehavior(timeoutMillis = 5_000)
        val m = mediator(pipelineBehaviors = listOf(behavior)) { add(PingHandler()) }
        repeat(5) { i ->
            assertEquals("pong:$i", m.send(PingQuery("$i")))
        }
    }
}
