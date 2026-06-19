package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

/**
 * A [PipelineBehavior] that caches handler results by request key for a configurable TTL.
 *
 * On a cache hit the handler is skipped entirely and the cached value is returned.
 * On a miss the handler runs and the result is stored. Entries expire after [ttlMs]
 * milliseconds — the next call after expiry re-runs the handler and refreshes the entry.
 *
 * Best suited for **query** requests whose results change infrequently. Commands that
 * produce side-effects should not be cached.
 *
 *
 * @param ttlMs time-to-live for each entry in milliseconds. Defaults to 60 000 (1 minute).
 * @param keyFor function that produces a cache key from a request. Defaults to `toString()`.
 * @param order position in the behavior chain. Defaults to `0`.
 */
class CachingPipelineBehavior(
    val ttlMs: Long = 60_000L,
    val keyFor: (Request<*>) -> String = { it.toString() },
    override val order: Int = 0,
) : PipelineBehavior {

    init {
        require(ttlMs > 0) { "ttlMs must be > 0, was $ttlMs" }
    }

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, Entry>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val key = keyFor(request)
        mutex.withLock {
            val entry = cache[key]
            if (entry != null && !entry.isExpired()) return entry.value as TResult
        }
        val result = next(request)
        mutex.withLock {
            cache[key] = Entry(result, TimeSource.Monotonic.markNow(), ttlMs)
        }
        return result
    }

    /** Removes the cached entry for [key]. No-op if the key is not cached. */
    suspend fun invalidate(key: String): Unit = mutex.withLock { cache.remove(key); Unit }

    /** Removes all cached entries. */
    suspend fun clear() = mutex.withLock { cache.clear() }

    /** Returns the number of entries currently in the cache (including expired ones not yet evicted). */
    suspend fun size(): Int = mutex.withLock { cache.size }

    private data class Entry(val value: Any?, val mark: TimeSource.Monotonic.ValueTimeMark, val ttlMs: Long) {
        fun isExpired() = mark.elapsedNow().inWholeMilliseconds >= ttlMs
    }
}
