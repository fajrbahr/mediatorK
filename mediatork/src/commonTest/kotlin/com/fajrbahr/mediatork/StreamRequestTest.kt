package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.feature.streamBehavior
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamRequestTest {

    private data class NumbersQuery(val count: Int) : StreamRequest<Int>

    private fun streamMediator(
        streamBehaviors: List<StreamPipelineBehavior> = emptyList(),
        handlers: HandlerRegistry.() -> Unit,
    ) = MediatorFactory.create(
        registry = HandlerRegistry().apply(handlers),
        streamPipelineBehaviors = streamBehaviors,
    )

    @Test
    fun `stream dispatches to handler and emits all items`() = runTest {
        val m = streamMediator {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    (1..request.count).asFlow()
            })
        }
        assertEquals(listOf(1, 2, 3), m.stream(NumbersQuery(3)).toList())
    }

    @Test
    fun `stream with empty flow emits no items`() = runTest {
        val m = streamMediator {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    emptyFlow()
            })
        }
        assertTrue(m.stream(NumbersQuery(0)).toList().isEmpty())
    }

    @Test
    fun `stream throws MissingStreamHandlerException when no handler registered`() {
        val m = MediatorFactory.create(registry = HandlerRegistry())
        assertFailsWith<MissingStreamHandlerException> {
            m.stream(NumbersQuery(1))
        }
    }

    @Test
    fun `MissingStreamHandlerException message contains request type name`() {
        val m = MediatorFactory.create(registry = HandlerRegistry())
        val ex = assertFailsWith<MissingStreamHandlerException> {
            m.stream(NumbersQuery(1))
        }
        assertTrue(ex.message!!.contains("NumbersQuery"))
    }

    @Test
    fun `stream pipeline behavior is invoked`() = runTest {
        var called = false
        val trackingBehavior = object : StreamPipelineBehavior {
            override fun <TRequest : StreamRequest<T>, T> process(
                requestContext: RequestContext,
                next: StreamHandlerDelegate<TRequest, T>,
                request: TRequest,
            ): Flow<T> {
                called = true
                return next(request)
            }
        }
        val m = streamMediator(streamBehaviors = listOf(trackingBehavior)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    (1..2).asFlow()
            })
        }
        m.stream(NumbersQuery(2)).toList()
        assertTrue(called)
    }

    @Test
    fun `stream pipeline behavior with isEnabled false is skipped`() = runTest {
        var called = false
        val disabledBehavior = object : StreamPipelineBehavior {
            override val isEnabled = false
            override fun <TRequest : StreamRequest<T>, T> process(
                requestContext: RequestContext,
                next: StreamHandlerDelegate<TRequest, T>,
                request: TRequest,
            ): Flow<T> {
                called = true
                return next(request)
            }
        }
        val m = streamMediator(streamBehaviors = listOf(disabledBehavior)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    (1..2).asFlow()
            })
        }
        m.stream(NumbersQuery(2)).toList()
        assertFalse(called)
    }

    @Test
    fun `stream pipeline behavior with appliesTo false is skipped`() = runTest {
        var called = false
        val selectiveBehavior = object : StreamPipelineBehavior {
            override fun appliesTo(request: StreamRequest<*>) = false
            override fun <TRequest : StreamRequest<T>, T> process(
                requestContext: RequestContext,
                next: StreamHandlerDelegate<TRequest, T>,
                request: TRequest,
            ): Flow<T> {
                called = true
                return next(request)
            }
        }
        val m = streamMediator(streamBehaviors = listOf(selectiveBehavior)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    (1..2).asFlow()
            })
        }
        m.stream(NumbersQuery(2)).toList()
        assertFalse(called)
    }

    @Test
    fun `multiple stream behaviors compose outermost-first by order`() = runTest {
        val executionOrder = mutableListOf<Int>()

        fun trackingBehavior(id: Int, orderVal: Int) = object : StreamPipelineBehavior {
            override val order = orderVal
            override fun <TRequest : StreamRequest<T>, T> process(
                requestContext: RequestContext,
                next: StreamHandlerDelegate<TRequest, T>,
                request: TRequest,
            ): Flow<T> {
                executionOrder += id
                return next(request)
            }
        }

        val m = streamMediator(
            streamBehaviors = listOf(trackingBehavior(id = 2, orderVal = 20), trackingBehavior(id = 1, orderVal = 10)),
        ) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    (1..1).asFlow()
            })
        }
        m.stream(NumbersQuery(1)).toList()
        assertEquals(listOf(1, 2), executionOrder)
    }

    @Test
    fun `stream behavior can transform flow items`() = runTest {
        val doubling = object : StreamPipelineBehavior {
            override fun <TRequest : StreamRequest<T>, T> process(
                requestContext: RequestContext,
                next: StreamHandlerDelegate<TRequest, T>,
                request: TRequest,
            ): Flow<T> {
                @Suppress("UNCHECKED_CAST")
                return (next(request) as Flow<Int>).map { it * 2 } as Flow<T>
            }
        }
        val m = streamMediator(streamBehaviors = listOf(doubling)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    (1..3).asFlow()
            })
        }
        assertEquals(listOf(2, 4, 6), m.stream(NumbersQuery(3)).toList())
    }

    @Test
    fun `stream behavior is invoked for each stream call`() = runTest {
        var invokeCount = 0
        val counting = object : StreamPipelineBehavior {
            override fun <TRequest : StreamRequest<T>, T> process(
                requestContext: RequestContext,
                next: StreamHandlerDelegate<TRequest, T>,
                request: TRequest,
            ): Flow<T> {
                invokeCount++
                return next(request)
            }
        }
        val m = streamMediator(streamBehaviors = listOf(counting)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> =
                    (1..1).asFlow()
            })
        }
        m.stream(NumbersQuery(1)).toList()
        m.stream(NumbersQuery(1)).toList()
        assertEquals(2, invokeCount)
    }

    @Test
    fun `stream handler receives correct request values`() = runTest {
        var receivedCount = -1
        val m = streamMediator {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: NumbersQuery
                ): Flow<Int> {
                    receivedCount = request.count
                    return (1..request.count).asFlow()
                }
            })
        }
        m.stream(NumbersQuery(7)).toList()
        assertEquals(7, receivedCount)
    }

    @Test
    fun `MissingStreamHandlerException is a MediatorException subtype`() {
        val m = MediatorFactory.create(registry = HandlerRegistry())
        assertFailsWith<MediatorException> {
            m.stream(NumbersQuery(1))
        }
    }

    // ── DSL streamBehavior() tests ───────────────────────────────────────────

    @Test
    fun `dsl streamBehavior wraps the handler`() = runTest {
        var called = false
        val b = streamBehavior { _, next, request ->
            called = true
            next(request)
        }
        val m = streamMediator(streamBehaviors = listOf(b)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery) =
                    (1..request.count).asFlow()
            })
        }
        assertEquals(listOf(1, 2, 3), m.stream(NumbersQuery(3)).toList())
        assertTrue(called)
    }

    @Test
    fun `dsl streamBehavior respects order`() = runTest {
        val executionOrder = mutableListOf<Int>()
        val first = streamBehavior(order = 10) { _, next, request ->
            executionOrder += 1; next(request)
        }
        val second = streamBehavior(order = 20) { _, next, request ->
            executionOrder += 2; next(request)
        }
        val m = streamMediator(streamBehaviors = listOf(second, first)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery) =
                    (1..1).asFlow()
            })
        }
        m.stream(NumbersQuery(1)).toList()
        assertEquals(listOf(1, 2), executionOrder)
    }

    @Test
    fun `dsl streamBehavior appliesTo filters requests`() = runTest {
        var called = false
        val b = streamBehavior(appliesTo = { false }) { _, next, request ->
            called = true; next(request)
        }
        val m = streamMediator(streamBehaviors = listOf(b)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery) =
                    (1..2).asFlow()
            })
        }
        m.stream(NumbersQuery(2)).toList()
        assertFalse(called)
    }

    @Test
    fun `dsl streamBehavior can transform flow`() = runTest {
        @Suppress("UNCHECKED_CAST")
        val doubling = streamBehavior { _, next, request ->
            (next(request) as Flow<Int>).map { it * 3 } as Flow<Nothing>
        }
        val m = streamMediator(streamBehaviors = listOf(doubling)) {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery) =
                    (1..3).asFlow()
            })
        }
        assertEquals(listOf(3, 6, 9), m.stream(NumbersQuery(3)).toList())
    }
}
