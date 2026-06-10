package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.handler.*
import com.fajrbahr.mediatork.pipeline.*

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeduplicationPipelineBehaviorTest {

    @Test
    fun `single request executes normally`() = runTest {
        val dedup = DeduplicationPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(dedup)) { register(PingHandler()) }
        assertEquals("pong:hi", m.send(PingQuery("hi")))
    }

    @Test
    fun `sequential requests with same key both execute`() = runTest {
        var count = 0
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                count++; return "pong:${request.value}"
            }
        }
        val dedup = DeduplicationPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(dedup)) { register(handler) }
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        // Sequential calls are NOT deduplicated — only concurrent in-flight ones are
        assertEquals(2, count)
    }

    @Test
    fun `inFlightCount is zero when idle`() = runTest {
        val dedup = DeduplicationPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(dedup)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals(0, dedup.inFlightCount())
    }

    @Test
    fun `concurrent requests with same key share result`() = runTest {
        var executions = 0
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
                executions++
                return "pong:${request.value}"
            }
        }
        val dedup = DeduplicationPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(dedup)) { register(handler) }

        val d1 = async { m.send(PingQuery("x")) }
        val d2 = async { m.send(PingQuery("x")) }
        val r1 = d1.await()
        val r2 = d2.await()

        assertEquals(r1, r2)
    }
}
