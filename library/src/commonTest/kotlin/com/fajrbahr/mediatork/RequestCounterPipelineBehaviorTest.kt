package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.pipeline.*

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
}
