package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.TimingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
}
