package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryPipelineBehaviorTest {

    @Test
    fun `succeeds on first attempt when no failure`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(retry(maxRetries = 3))
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `retries and succeeds after transient failures`() = runTest {
        var calls = 0
        val m = mediatorK {
            handle<PingQuery, String> {
                calls++
                if (calls <= 2) throw RuntimeException("attempt $calls failed")
                "pong:${it.value}"
            }
            behaviors(retry(maxRetries = 3))
        }
        assertEquals("pong:hi", m.send(PingQuery("hi")))
    }

    @Test
    fun `rethrows after exhausting all retries`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("always fails") }
            behaviors(retry(maxRetries = 2))
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
    }

    private class NonRetryableException(message: String) : Exception(message)

    @Test
    fun `does not retry when retryOn returns false`() = runTest {
        var attempts = 0
        val m = mediatorK {
            handle<PingQuery, String> {
                attempts++
                throw NonRetryableException("non-retryable")
            }
            behaviors(retry(maxRetries = 3, retryOn = { it is RuntimeException }))
        }
        assertFailsWith<NonRetryableException> { m.send(PingQuery("x")) }
        assertEquals(1, attempts)
    }

    @Test
    fun `maxRetries=0 means exactly one attempt`() = runTest {
        var calls = 0
        val m = mediatorK {
            handle<PingQuery, String> {
                calls++
                if (calls <= 1) throw RuntimeException("attempt $calls failed")
                "pong:${it.value}"
            }
            behaviors(retry(maxRetries = 0))
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
    }
}
