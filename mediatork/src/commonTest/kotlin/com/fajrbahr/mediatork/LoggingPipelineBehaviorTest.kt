package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoggingPipelineBehaviorTest {

    @Test
    fun `logs request name on entry and exit with result`() = runTest {
        val log = mutableListOf<String>()
        val m = mediator(pipelineBehaviors = listOf(LoggingPipelineBehavior(logger = log::add))) {
            register(PingHandler())
        }
        m.send(PingQuery("hello"))
        assertEquals(listOf("→ PingQuery", "← PingQuery result=pong:hello"), log)
    }

    @Test
    fun `default order is -100`() {
        assertEquals(-100, LoggingPipelineBehavior().order)
    }
}
