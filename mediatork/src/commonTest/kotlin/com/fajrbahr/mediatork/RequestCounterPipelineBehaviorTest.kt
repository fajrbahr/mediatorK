package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.RequestCounterPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestCounterPipelineBehaviorTest {

    @Test
    fun `counts a single request type`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(counter)) { register(PingHandler()) }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(2L, counter.countFor(PingQuery::class))
    }

    @Test
    fun `counts multiple request types independently`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(counter)) {
            register(PingHandler())
            register(AddHandler())
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        m.send(AddCommand(3, 4))
        assertEquals(1L, counter.countFor(PingQuery::class))
        assertEquals(2L, counter.countFor(AddCommand::class))
    }

    @Test
    fun `snapshot returns all counts`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(counter)) {
            register(PingHandler())
            register(AddHandler())
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        val snap = counter.snapshot()
        assertEquals(1L, snap["PingQuery"])
        assertEquals(1L, snap["AddCommand"])
    }

    @Test
    fun `reset clears all counts`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(counter)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        counter.reset()
        assertEquals(0L, counter.countFor(PingQuery::class))
        assertEquals(emptyMap(), counter.snapshot())
    }

    @Test
    fun `returns zero for unseen request type`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        assertEquals(0L, counter.countFor(PingQuery::class))
    }

    @Test
    fun `snapshot is empty initially`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        assertEquals(emptyMap(), counter.snapshot())
    }

    @Test
    fun `count increments even when handler throws`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(counter)) {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: PingQuery,
                ): String = throw RuntimeException("always fails")
            })
        }
        runCatching { m.send(PingQuery("x")) }
        assertEquals(1L, counter.countFor(PingQuery::class))
    }

    @Test
    fun `default order is 0`() {
        assertEquals(0, RequestCounterPipelineBehavior().order)
    }

    @Test
    fun `custom order value is reflected on instance`() {
        assertEquals(5, RequestCounterPipelineBehavior(order = 5).order)
    }

    @Test
    fun `reset leaves snapshot empty`() = runTest {
        val counter = RequestCounterPipelineBehavior()
        val m = mediator(pipelineBehaviors = listOf(counter)) {
            register(PingHandler())
            register(AddHandler())
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        counter.reset()
        assertEquals(emptyMap(), counter.snapshot())
    }
}
