package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.pipeline.*

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RateLimitPipelineBehaviorTest {

    @Test
    fun `allows requests within limit`() = runTest {
        val rl = RateLimitPipelineBehavior(maxRequests = 3, windowMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(rl)) { register(PingHandler()) }
        repeat(3) { m.send(PingQuery("x")) }
    }

    @Test
    fun `throws when limit exceeded`() = runTest {
        val rl = RateLimitPipelineBehavior(maxRequests = 2, windowMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(rl)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        assertFailsWith<RateLimitExceededException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `limits are per request type`() = runTest {
        val rl = RateLimitPipelineBehavior(maxRequests = 1, windowMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(rl)) {
            register(PingHandler())
            register(AddHandler())
        }
        m.send(PingQuery("x"))   // 1st PingQuery — ok
        m.send(AddCommand(1, 2)) // 1st AddCommand — ok (separate window)
        assertFailsWith<RateLimitExceededException> { m.send(PingQuery("y")) } // 2nd PingQuery — blocked
    }

    @Test
    fun `exception message contains request name and limits`() = runTest {
        val rl = RateLimitPipelineBehavior(maxRequests = 1, windowMs = 500)
        val m = mediator(pipelineBehaviors = listOf(rl)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        val ex = assertFailsWith<RateLimitExceededException> { m.send(PingQuery("y")) }
        assertEquals("PingQuery", ex.requestName)
        assertEquals(1, ex.maxRequests)
    }
}
