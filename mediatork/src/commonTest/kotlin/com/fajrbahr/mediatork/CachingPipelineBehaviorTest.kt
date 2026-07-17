package com.fajrbahr.mediatork

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CachingPipelineBehaviorTest {

    private var handlerCallCount = 0

    private fun buildMediatorWithcache(cache: CacheBehavior) = mediatorK {
        handle<PingQuery, String> {
            handlerCallCount++
            "pong:${it.value}"
        }
        behaviors(cache)
    }

    @Test
    fun `returns cached result on second call`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        assertEquals(1, handlerCallCount)
    }

    @Test
    fun `different keys do not share cache entries`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(2, handlerCallCount)
    }

    @Test
    fun `invalidate forces re-execution`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        val req = PingQuery("x")
        m.send(req)
        cache.invalidate(req.toString())
        m.send(req)
        assertEquals(2, handlerCallCount)
    }

    @Test
    fun `clear forces re-execution for all keys`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        cache.clear()
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(4, handlerCallCount)
    }

    @Test
    fun `size reflects number of cached entries`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `zero ttlMs throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { cache(ttlMs = 0) }
    }

    @Test
    fun `negative ttlMs throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { cache(ttlMs = -1) }
    }

    @Test
    fun `custom keyFor groups different requests under the same cache key`() = runTest {
        val cache = cache(ttlMs = 60_000, keyFor = { "fixed-key" })
        val m = mediatorK {
            handle<PingQuery, String> {
                handlerCallCount++
                "pong:${it.value}"
            }
            behaviors(cache)
        }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(1, handlerCallCount)
    }

    @Test
    fun `size is zero before any request is sent`() = runTest {
        val cache = cache(ttlMs = 60_000)
        assertEquals(0, cache.size())
    }

    @Test
    fun `size decrements after invalidate`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        val req = PingQuery("x")
        m.send(req)
        assertEquals(1, cache.size())
        cache.invalidate(req.toString())
        assertEquals(0, cache.size())
    }

    @Test
    fun `cached result equals original handler result`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        val first = m.send(PingQuery("z"))
        val second = m.send(PingQuery("z"))
        assertEquals(first, second)
        assertEquals(1, handlerCallCount)
    }

    @Test
    fun `invalidate of unknown key is a no-op`() = runTest {
        val cache = cache(ttlMs = 60_000)
        cache.invalidate("nonexistent-key")
        assertEquals(0, cache.size())
    }

    @Test
    fun `clear on empty cache is a no-op`() = runTest {
        val cache = cache(ttlMs = 60_000)
        cache.clear()
        assertEquals(0, cache.size())
    }

    @Test
    fun `result is returned correctly after cache hit`() = runTest {
        val cache = cache(ttlMs = 60_000)
        val m = buildMediatorWithcache(cache)
        assertEquals("pong:hi", m.send(PingQuery("hi")))
        assertEquals("pong:hi", m.send(PingQuery("hi")))
    }

    @Test
    fun `filter excludes matching requests from caching`() = runTest {
        val cache = cache(
            ttlMs = 60_000,
            filter = { it !is PingQuery },
        )
        val m = mediatorK {
            handle<PingQuery, String> {
                handlerCallCount++
                "pong:${it.value}"
            }
            behaviors(cache)
        }
        m.send(PingQuery("x"))
        m.send(PingQuery("x"))
        assertEquals(2, handlerCallCount, "excluded request should run the handler every time")
    }

    @Test
    fun `filter allows non-excluded requests to be cached`() = runTest {
        var addCallCount = 0
        val cache = cache(
            ttlMs = 60_000,
            filter = { it !is PingQuery },
        )
        val m = mediatorK {
            handle<PingQuery, String> {
                handlerCallCount++
                "pong:${it.value}"
            }
            handle<AddCommand, Int> {
                addCallCount++
                it.a + it.b
            }
            behaviors(cache)
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
        val cache = cache(
            ttlMs = 60_000,
            filter = { it !is PingQuery },
        )
        val m = mediatorK {
            handle<PingQuery, String> {
                handlerCallCount++
                "pong:${it.value}"
            }
            behaviors(cache)
        }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(0, cache.size(), "excluded requests should not be stored")
    }
}
