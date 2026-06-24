package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class StreamRequestTest {

    private data class NumbersQuery(val count: Int) : StreamRequest<Int>

    private fun streamMediator(
        streamBehaviors: List<StreamPipelineBehavior> = emptyList(),
        handlers: HandlerRegistry.() -> Unit,
    ) = MediatorFactory.create(
        registrars = listOf(object : MediatorRegistrar {
            override fun register(registry: HandlerRegistry) = registry.handlers()
        }),
        streamPipelineBehaviors = streamBehaviors,
        verifyHandlers = false,
    )

    @Test
    fun `stream dispatches to handler and emits all items`() = runTest {
        val m = streamMediator {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery): Flow<Int> =
                    (1..request.count).asFlow()
            })
        }
        assertEquals(listOf(1, 2, 3), m.stream(NumbersQuery(3)).toList())
    }

    @Test
    fun `stream with empty flow emits no items`() = runTest {
        val m = streamMediator {
            registerStream(object : StreamRequestHandler<NumbersQuery, Int> {
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery): Flow<Int> =
                    emptyFlow()
            })
        }
        assertTrue(m.stream(NumbersQuery(0)).toList().isEmpty())
    }

    @Test
    fun `stream throws MissingStreamHandlerException when no handler registered`() {
        val m = MediatorFactory.create(registrars = emptyList(), verifyHandlers = false)
        assertFailsWith<MissingStreamHandlerException> {
            m.stream(NumbersQuery(1))
        }
    }

    @Test
    fun `MissingStreamHandlerException message contains request type name`() {
        val m = MediatorFactory.create(registrars = emptyList(), verifyHandlers = false)
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
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery): Flow<Int> =
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
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery): Flow<Int> =
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
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery): Flow<Int> =
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
                override fun handle(mediator: Mediator, requestContext: RequestContext, request: NumbersQuery): Flow<Int> =
                    (1..1).asFlow()
            })
        }
        m.stream(NumbersQuery(1)).toList()
        assertEquals(listOf(1, 2), executionOrder)
    }
}
