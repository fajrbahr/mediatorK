package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineBehaviorTest {

    private fun loggingBehavior(order: Int, label: String, log: MutableList<String>) =
        behavior(order = order) { _, _, next ->
            log += "$label-before"
            next().also { log += "$label-after" }
        }

    @Test
    fun `single behavior wraps the handler`() = runTest {
        val log = mutableListOf<String>()
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(loggingBehavior(0, "b", log))
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("b-before", "b-after"), log)
    }

    @Test
    fun `lower order behavior is outermost`() = runTest {
        val log = mutableListOf<String>()
        val outer = loggingBehavior(-10, "outer", log)
        val inner = loggingBehavior(10, "inner", log)
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(inner, outer)
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("outer-before", "inner-before", "inner-after", "outer-after"), log)
    }

    @Test
    fun `behaviors with equal order run in registered order`() = runTest {
        val log = mutableListOf<String>()
        val b1 = loggingBehavior(0, "b1", log)
        val b2 = loggingBehavior(0, "b2", log)
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(b1, b2)
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("b1-before", "b2-before", "b2-after", "b1-after"), log)
    }

    @Test
    fun `behavior with appliesTo=false is skipped`() = runTest {
        var ran = false
        val selective = behavior(appliesTo = { false }) { _, _, next ->
            ran = true; next()
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(selective)
        }
        m.send(PingQuery("x"))
        assertFalse(ran)
    }

    @Test
    fun `behavior with appliesTo=true runs`() = runTest {
        var ran = false
        val b = behavior(appliesTo = { true }) { _, _, next ->
            ran = true; next()
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(b)
        }
        m.send(PingQuery("x"))
        assertTrue(ran)
    }

    @Test
    fun `behavior with isEnabled=false is skipped`() = runTest {
        var ran = false
        val disabled = behavior(isEnabled = false) { _, _, next ->
            ran = true; next()
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(disabled)
        }
        m.send(PingQuery("x"))
        assertFalse(ran)
    }

    @Test
    fun `behavior can read and write request context`() = runTest {
        val b = behavior { _, context, next ->
            context.put("from-behavior", "injected")
            next()
        }
        var captured: String? = null
        val m = mediatorK {
            handle<PingQuery, String> {
                captured = context.getMetaData("from-behavior")
                "ok"
            }
            behaviors(b)
        }
        m.send(PingQuery("x"))
        assertEquals("injected", captured)
    }

    @Test
    fun `behavior can short-circuit without calling next`() = runTest {
        var handlerRan = false
        val shortCircuit = behavior { _, _, _ ->
            "short-circuited"
        }
        val m = mediatorK {
            handle<PingQuery, String> {
                handlerRan = true
                "handler"
            }
            behaviors(shortCircuit)
        }
        val result = m.send(PingQuery("x"))
        assertEquals("short-circuited", result)
        assertFalse(handlerRan)
    }

    @Test
    fun `behavior appliesTo can restrict to specific request type`() = runTest {
        var ranForPing = false
        val pingOnly = behavior(appliesTo = { it is PingQuery }) { _, _, next ->
            ranForPing = true; next()
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
            behaviors(pingOnly)
        }
        m.send(AddCommand(1, 2))
        assertFalse(ranForPing)
        m.send(PingQuery("x"))
        assertTrue(ranForPing)
    }

    @Test
    fun `result flows through behaviors unchanged when unmodified`() = runTest {
        val b = loggingBehavior(0, "b", mutableListOf())
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(b)
        }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }
}
