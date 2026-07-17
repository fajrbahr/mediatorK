package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MediatorTest {

    @Test
    fun `send returns handler result`() = runTest {
        val m = mediatorK { handle<PingQuery, String> { "pong:${it.value}" } }
        assertEquals("pong:hello", m.send(PingQuery("hello")))
    }

    @Test
    fun `send routes to correct handler among many`() = runTest {
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            handle<AddCommand, Int> { it.a + it.b }
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
        assertEquals("pong:x", m.send(PingQuery("x")))
    }

    @Test
    fun `send with Request_Unit returns Unit`() = runTest {
        var lastId: String? = null
        val m = mediatorK {
            handle<NoResultCommand, Unit> { lastId = it.id }
        }
        m.send(NoResultCommand("id-1"))
        assertEquals("id-1", lastId)
    }

    @Test
    fun `send throws MissingHandlerException when no handler registered`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MissingHandlerException> {
            m.send(PingQuery("x"))
        }
    }

    @Test
    fun `MissingHandlerException message includes request type name`() = runTest {
        val m = mediatorK { }
        val ex = assertFailsWith<MissingHandlerException> { m.send(PingQuery("x")) }
        assertTrue(ex.message!!.contains("PingQuery"))
    }

    @Test
    fun `publish delivers notification to all registered handlers`() = runTest {
        val received1 = mutableListOf<String>()
        val received2 = mutableListOf<String>()
        val m = mediatorK {
            notification<PingNotification> { received1 += it.message }
            notification<PingNotification> { received2 += it.message }
        }
        m.publish(PingNotification("hello"))
        assertEquals(listOf("hello"), received1)
        assertEquals(listOf("hello"), received2)
    }

    @Test
    fun `publish with no handlers throws MissingNotificationHandlerException`() = runTest {
        val m = mediatorK { }
        assertFailsWith<MissingNotificationHandlerException> {
            m.publish(PingNotification("silent"))
        }
    }

    @Test
    fun `pipeline behavior wraps handler in order`() = runTest {
        val log = mutableListOf<String>()

        val outer = behavior(order = -10) { _, _, next ->
            log += "outer-before"
            next().also { log += "outer-after" }
        }

        val inner = behavior(order = 10) { _, _, next ->
            log += "inner-before"
            next().also { log += "inner-after" }
        }

        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(outer, inner)
        }

        m.send(PingQuery("x"))
        assertEquals(listOf("outer-before", "inner-before", "inner-after", "outer-after"), log)
    }

    @Test
    fun `pipeline behavior with appliesTo=false is skipped`() = runTest {
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
    fun `PRE behavior runs before handler and can populate context`() = runTest {
        var contextValue: String? = null

        val pre = behavior { _, context, next ->
            context.put("key", "injected"); next()
        }

        val m = mediatorK {
            handle<PingQuery, String> {
                contextValue = context.getMetaData("key"); "ok"
            }
            behaviors(pre)
        }
        m.send(PingQuery("x"))
        assertEquals("injected", contextValue)
    }

    @Test
    fun `POST behavior runs after handler and receives response`() = runTest {
        var captured: Any? = "not-set"

        val post = behavior { _, _, next ->
            val r = next(); captured = r; r
        }

        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(post)
        }
        m.send(PingQuery("world"))
        assertEquals("pong:world", captured)
    }
}
