package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RateLimitPipelineBehaviorTest {

    @Test
    fun `allows requests within limit`() = runTest {
        val rl = rateLimit(maxRequests = 3, windowMs = 60_000)
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(rl)
        }
        repeat(3) { m.send(PingQuery("x")) }
    }

    @Test
    fun `throws when limit exceeded`() = runTest {
        val rl = rateLimit(maxRequests = 2, windowMs = 60_000)
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(rl)
        }
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        assertFailsWith<RateLimitExceededException> { m.send(PingQuery("x")) }
    }

    @Test
    fun `limits are per request type`() = runTest {
        val rl = rateLimit(maxRequests = 1, windowMs = 60_000)
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(rl)
        }
        m.send(PingQuery("x"))   // 1st PingQuery — ok
        m.send(AddCommand(1, 2)) // 1st AddCommand — ok (separate window)
        assertFailsWith<RateLimitExceededException> { m.send(PingQuery("y")) } // 2nd PingQuery — blocked
    }

    @Test
    fun `exception message contains request name and limits`() = runTest {
        val rl = rateLimit(maxRequests = 1, windowMs = 500)
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(rl)
        }
        m.send(PingQuery("x"))
        val ex = assertFailsWith<RateLimitExceededException> { m.send(PingQuery("y")) }
        assertEquals("PingQuery", ex.requestName)
        assertEquals(1, ex.maxRequests)
    }
}
