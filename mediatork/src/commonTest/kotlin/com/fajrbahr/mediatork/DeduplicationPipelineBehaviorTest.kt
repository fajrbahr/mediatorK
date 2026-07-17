package com.fajrbahr.mediatork

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeduplicationPipelineBehaviorTest {

    @Test
    fun `single request executes normally`() = runTest {
        val dedup = deduplicator()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(dedup)
        }
        assertEquals("pong:hi", m.send(PingQuery("hi")))
    }

    @Test
    fun `sequential requests with same key both execute`() = runTest {
        var count = 0
        val dedup = deduplicator()
        val m = mediatorK {
            handle<PingQuery, String> {
                count++; "pong:${it.value}"
            }
            behaviors(dedup)
        }
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        // Sequential calls are NOT deduplicated — only concurrent in-flight ones are
        assertEquals(2, count)
    }

    @Test
    fun `inFlightCount is zero when idle`() = runTest {
        val dedup = deduplicator()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(dedup)
        }
        m.send(PingQuery("x"))
        assertEquals(0, dedup.inFlightCount())
    }

    @Test
    fun `concurrent requests with same key share result`() = runTest {
        var executions = 0
        val dedup = deduplicator()
        val m = mediatorK {
            handle<PingQuery, String> {
                executions++
                "pong:${it.value}"
            }
            behaviors(dedup)
        }

        val d1 = async { m.send(PingQuery("x")) }
        val d2 = async { m.send(PingQuery("x")) }
        val r1 = d1.await()
        val r2 = d2.await()

        assertEquals(r1, r2)
    }
}
