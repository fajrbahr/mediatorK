package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.CachingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    @Test
    fun `zero ttlMs throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { CachingPipelineBehavior(ttlMs = 0) }
    }

    @Test
    fun `negative ttlMs throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { CachingPipelineBehavior(ttlMs = -1) }
    }

    @Test
    fun `custom keyFor groups different requests under the same cache key`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000, keyFor = { "fixed-key" })
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(1, handlerCallCount)
    }

    @Test
    fun `size is zero before any request is sent`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        assertEquals(0, cache.size())
    }

    @Test
    fun `size decrements after invalidate`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        val req = PingQuery("x")
        m.send(req)
        assertEquals(1, cache.size())
        cache.invalidate(req.toString())
        assertEquals(0, cache.size())
    }

    @Test
    fun `cached result equals original handler result`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        val first = m.send(PingQuery("z"))
        val second = m.send(PingQuery("z"))
        assertEquals(first, second)
        assertEquals(1, handlerCallCount)
    }

    @Test
    fun `invalidate of unknown key is a no-op`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        cache.invalidate("nonexistent-key")
        assertEquals(0, cache.size())
    }

    @Test
    fun `clear on empty cache is a no-op`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        cache.clear()
        assertEquals(0, cache.size())
    }

    @Test
    fun `result is returned correctly after cache hit`() = runTest {
        val cache = CachingPipelineBehavior(ttlMs = 60_000)
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        assertEquals("pong:hi", m.send(PingQuery("hi")))
        assertEquals("pong:hi", m.send(PingQuery("hi")))
    }

    @Test
    fun `filter excludes matching requests from caching`() = runTest {
        val cache = CachingPipelineBehavior(
            ttlMs = 60_000,
            filter = { it !is PingQuery },
        )
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        assertEquals(2, handlerCallCount, "excluded request should run the handler every time")
    }

    @Test
    fun `filter allows non-excluded requests to be cached`() = runTest {
        var addCallCount = 0
        val addHandler = object : RequestHandler<AddCommand, Int> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: AddCommand): Int {
                addCallCount++
                return request.a + request.b
            }
        }
        val cache = CachingPipelineBehavior(
            ttlMs = 60_000,
            filter = { it !is PingQuery },
        )
        val m = mediator(pipelineBehaviors = listOf(cache)) {
            register(countingHandler())
            register(addHandler)
        }

        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        assertEquals(2, handlerCallCount, "PingQuery should bypass cache")

        m.send(AddCommand(1, 2))
        m.send(AddCommand(1, 2))
        assertEquals(1, addCallCount, "AddCommand should be cached")
    }

    @Test
    fun `excluded requests do not appear in cache size`() = runTest {
        val cache = CachingPipelineBehavior(
            ttlMs = 60_000,
            filter = { it !is PingQuery },
        )
        val m = mediator(pipelineBehaviors = listOf(cache)) { register(countingHandler()) }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(0, cache.size(), "excluded requests should not be stored")
    }
}
