package com.fajrbahr.mediatork.feature

import com.fajrbahr.mediatork.HandlerScope
import com.fajrbahr.mediatork.LambdaRequestHandler
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.measureTimedValue

class TimeoutException(message: String) : Exception(message)

// ── Decorator handlers ──────────────────────────────────────────────────────

internal class RetryHandler<TRequest : Request<TResult>, TResult>(
    private val inner: RequestHandler<TRequest, TResult>,
    private val maxAttempts: Int,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return inner.handle(mediator, requestContext, request)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError!!
    }
}

internal class TimeoutHandler<TRequest : Request<TResult>, TResult>(
    private val inner: RequestHandler<TRequest, TResult>,
    private val duration: Duration,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        return withTimeoutOrNull(duration) {
            inner.handle(mediator, requestContext, request)
        } ?: throw TimeoutException(
            "Handler timed out after $duration"
        )
    }
}

internal class MeasureHandler<TRequest : Request<TResult>, TResult>(
    private val inner: RequestHandler<TRequest, TResult>,
    private val onMeasured: suspend (TRequest, Long) -> Unit,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        val (result, elapsed) = measureTimedValue {
            inner.handle(mediator, requestContext, request)
        }
        onMeasured(request, elapsed.inWholeMilliseconds)
        return result
    }
}

fun interface HandlerLogger<in TRequest, in TResult> {
    fun log(phase: String, request: TRequest?, result: TResult?, error: Throwable?)
}

internal class PrintHandlerLogger<TRequest, TResult> : HandlerLogger<TRequest, TResult> {
    override fun log(phase: String, request: TRequest?, result: TResult?, error: Throwable?) {
        when (phase) {
            "before" -> println("[LOG] → ${request!!::class.simpleName}")
            "after" -> println("[LOG] ← $result")
            "error" -> println("[LOG] ✗ ${error?.message}")
        }
    }
}

internal class LogHandler<TRequest : Request<TResult>, TResult>(
    private val inner: RequestHandler<TRequest, TResult>,
    private val logger: HandlerLogger<TRequest, TResult>,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        logger.log("before", request, null, null)
        return try {
            val result = inner.handle(mediator, requestContext, request)
            logger.log("after", request, result, null)
            result
        } catch (e: Exception) {
            logger.log("error", request, null, e)
            throw e
        }
    }
}

internal class CacheHandler<TRequest : Request<TResult>, TResult>(
    private val inner: RequestHandler<TRequest, TResult>,
    private val keyFrom: (TRequest) -> Any,
    private val store: MutableMap<Any, Any?>,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        val key = keyFrom(request)
        @Suppress("UNCHECKED_CAST")
        store[key]?.let { return it as TResult }
        val result = inner.handle(mediator, requestContext, request)
        store[key] = result
        return result
    }
}

internal class FallbackHandler<TRequest : Request<TResult>, TResult>(
    private val inner: RequestHandler<TRequest, TResult>,
    private val fallback: RequestHandler<TRequest, TResult>,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        return try {
            inner.handle(mediator, requestContext, request)
        } catch (_: Exception) {
            fallback.handle(mediator, requestContext, request)
        }
    }
}

// ── Feature-level composition ───────────────────────────────────────────────
// Usage: feature<Cmd, Result> { handle { ... } }.measure().log().retry(3)

private fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.withHandler(
    handler: RequestHandler<TRequest, TResult>,
) = Feature(handler, validators, beforeBlock, afterBlock)

fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.measure(
    onMeasured: suspend (TRequest, Long) -> Unit = { _, ms -> println("[MEASURE] completed in ${ms}ms") },
): Feature<TRequest, TResult> = withHandler(MeasureHandler(handler, onMeasured))

fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.log(
    logger: HandlerLogger<TRequest, TResult> = PrintHandlerLogger(),
): Feature<TRequest, TResult> = withHandler(LogHandler(handler, logger))

fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.cache(
    keyFrom: (TRequest) -> Any = { it },
    store: MutableMap<Any, Any?> = mutableMapOf(),
): Feature<TRequest, TResult> = withHandler(CacheHandler(handler, keyFrom, store))

fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.retry(
    maxAttempts: Int = 3,
): Feature<TRequest, TResult> = withHandler(RetryHandler(handler, maxAttempts))

fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.timeout(
    duration: Duration,
): Feature<TRequest, TResult> = withHandler(TimeoutHandler(handler, duration))

fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.fallback(
    handler: RequestHandler<TRequest, TResult>,
): Feature<TRequest, TResult> = withHandler(FallbackHandler(this.handler, handler))

fun <TRequest : Request<TResult>, TResult> Feature<TRequest, TResult>.fallback(
    block: suspend HandlerScope.(TRequest) -> TResult,
): Feature<TRequest, TResult> = fallback(
    @Suppress("UNCHECKED_CAST")
    LambdaRequestHandler(block) as RequestHandler<TRequest, TResult>
)
