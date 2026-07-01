package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.TimingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TimingPipelineBehaviorTest {

    @Test
    fun `callback receives request name and non-negative duration`() = runTest {
        var capturedName: String? = null
        var capturedMs: Long? = null
        val timing = TimingPipelineBehavior(onTiming = { name, ms -> capturedName = name; capturedMs = ms })
        val m = mediator(pipelineBehaviors = listOf(timing)) { register(PingHandler()) }
        m.send(PingQuery("x"))
        assertEquals("PingQuery", capturedName)
        assertNotNull(capturedMs)
        assertTrue(capturedMs!! >= 0)
    }

    @Test
    fun `callback is called even when handler throws`() = runTest {
        var called = false
        val timing = TimingPipelineBehavior(onTiming = { _, _ -> called = true })
        val handler = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(
                mediator: Mediator,
                requestContext: RequestContext,
                request: PingQuery
            ): String =
                throw RuntimeException("boom")
        }
        val m = mediator(pipelineBehaviors = listOf(timing)) { register(handler) }
        try {
            m.send(PingQuery("x"))
        } catch (_: RuntimeException) {
        }
        assertTrue(called)
    }

    @Test
    fun `callback is called for every request`() = runTest {
        var count = 0
        val timing = TimingPipelineBehavior(onTiming = { _, _ -> count++ })
        val m = mediator(pipelineBehaviors = listOf(timing)) { register(PingHandler()) }
        repeat(3) { m.send(PingQuery("x")) }
        assertEquals(3, count)
    }

    @Test
    fun `default order is 0`() {
        assertEquals(0, TimingPipelineBehavior(onTiming = { _, _ -> }).order)
    }

    @Test
    fun `custom order value is reflected on instance`() {
        assertEquals(-10, TimingPipelineBehavior(order = -10, onTiming = { _, _ -> }).order)
    }

    @Test
    fun `callback receives AddCommand class name`() = runTest {
        var capturedName: String? = null
        val timing = TimingPipelineBehavior(onTiming = { name, _ -> capturedName = name })
        val m = mediator(pipelineBehaviors = listOf(timing)) { register(AddHandler()) }
        m.send(AddCommand(1, 2))
        assertEquals("AddCommand", capturedName)
    }

    @Test
    fun `exception is rethrown after timing callback fires`() = runTest {
        val timing = TimingPipelineBehavior(onTiming = { _, _ -> })
        val m = mediator(pipelineBehaviors = listOf(timing)) {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(
                    mediator: Mediator,
                    requestContext: RequestContext,
                    request: PingQuery
                ): String =
                    throw IllegalStateException("domain error")
            })
        }
        val ex = assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
        assertEquals("domain error", ex.message)
    }

    @Test
    fun `result passes through unchanged`() = runTest {
        val timing = TimingPipelineBehavior(onTiming = { _, _ -> })
        val m = mediator(pipelineBehaviors = listOf(timing)) { register(PingHandler()) }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `two different request types each receive their own class name`() = runTest {
        val names = mutableListOf<String>()
        val timing = TimingPipelineBehavior(onTiming = { name, _ -> names += name })
        val m = mediator(pipelineBehaviors = listOf(timing)) {
            register(PingHandler())
            register(AddHandler())
        }
        m.send(PingQuery("x"))
        m.send(AddCommand(1, 2))
        assertEquals(listOf("PingQuery", "AddCommand"), names)
    }
}
