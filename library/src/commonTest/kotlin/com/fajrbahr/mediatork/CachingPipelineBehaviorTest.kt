package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.CachingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CachingPipelineBehaviorTest {

    private var handlerCallCount = 0

    private fun countingHandler() = object : RequestHandler<PingQuery, String> {
        override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String {
            handlerCallCount++
            return "pong:${request.value}"
        }
    }

    @Test
    fun `returns cached result on second call`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        assertEquals(1, handlerCallCount)
    }

    @Test
    fun `different keys do not share cache entries`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(2, handlerCallCount)
    }

    @Test
    fun `invalidate forces re-execution`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        val req = PingQuery("x")
        m.send(req)
        cache.invalidate(req.toString())
        m.send(req)
        assertEquals(2, handlerCallCount)
    }

    @Test
    fun `clear forces re-execution for all keys`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        cache.clear()
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(4, handlerCallCount)
    }

    @Test
    fun `size reflects number of cached entries`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(2, cache.size())
    }
}
