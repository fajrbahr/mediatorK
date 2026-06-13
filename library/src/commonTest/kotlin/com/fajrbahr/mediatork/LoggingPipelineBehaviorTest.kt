package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.pipeline.LoggingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggingPipelineBehaviorTest {

    @Test
    fun `logs request name on entry and exit`() = runTest {
        val log = mutableListOf<String>()
        val m = mediator(pipelineBehaviors = listOf(LoggingPipelineBehavior(logger = log::add))) {
            register(PingHandler())
        }
        m.send(PingQuery("hello"))
        assertEquals(listOf("→ PingQuery", "← PingQuery"), log)
    }

    @Test
    fun `logs result when logResult=true`() = runTest {
        val log = mutableListOf<String>()
        val m = mediator(pipelineBehaviors = listOf(LoggingPipelineBehavior(logger = log::add, logResult = true))) {
            register(PingHandler())
        }
        m.send(PingQuery("world"))
        assertTrue(log.any { it.contains("result=pong:world") })
    }

    @Test
    fun `does not log result when logResult=false`() = runTest {
        val log = mutableListOf<String>()
        val m = mediator(pipelineBehaviors = listOf(LoggingPipelineBehavior(logger = log::add, logResult = false))) {
            register(PingHandler())
        }
        m.send(PingQuery("x"))
        assertTrue(log.none { it.contains("result=") })
    }

    @Test
    fun `default order is -100`() {
        assertEquals(-100, LoggingPipelineBehavior().order)
    }
}
