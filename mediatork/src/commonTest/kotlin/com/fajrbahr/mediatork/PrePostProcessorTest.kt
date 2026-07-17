package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class PrePostProcessorTest {

    @Test
    fun `PRE behavior runs before handler`() = runTest {
        val order = mutableListOf<String>()
        val pre = behavior { _, _, next ->
            order += "pre"; next()
        }
        val m = mediatorK {
            handle<PingQuery, String> { order += "handler"; "ok" }
            behaviors(pre)
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("pre", "handler"), order)
    }

    @Test
    fun `PRE behavior can populate request context for handler`() = runTest {
        var captured: String? = null
        val pre = behavior { _, context, next ->
            context.put("token", "abc123"); next()
        }
        val m = mediatorK {
            handle<PingQuery, String> {
                captured = context.getMetaData("token"); "ok"
            }
            behaviors(pre)
        }
        m.send(PingQuery("x"))
        assertEquals("abc123", captured)
    }

    @Test
    fun `multiple PRE behaviors run in ascending order`() = runTest {
        val order = mutableListOf<String>()
        val first = behavior(order = 1) { _, _, next ->
            order += "first"; next()
        }
        val second = behavior(order = 2) { _, _, next ->
            order += "second"; next()
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(second, first)
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("first", "second"), order)
    }

    @Test
    fun `PRE behavior throwing aborts pipeline`() = runTest {
        var handlerRan = false
        val pre = behavior { _, _, _ ->
            throw IllegalArgumentException("invalid")
        }
        val m = mediatorK {
            handle<PingQuery, String> { handlerRan = true; "ok" }
            behaviors(pre)
        }
        assertFailsWith<IllegalArgumentException> { m.send(PingQuery("x")) }
        assertFalse(handlerRan)
    }

    @Test
    fun `POST behavior runs after handler`() = runTest {
        val order = mutableListOf<String>()
        val post = behavior { _, _, next ->
            val r = next(); order += "post"; r
        }
        val m = mediatorK {
            handle<PingQuery, String> { order += "handler"; "ok" }
            behaviors(post)
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("handler", "post"), order)
    }

    @Test
    fun `POST behavior receives handler response`() = runTest {
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

    @Test
    fun `POST behavior receives original request`() = runTest {
        var capturedRequest: Request<*>? = null
        val post = behavior { request, _, next ->
            val r = next(); capturedRequest = request; r
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(post)
        }
        m.send(PingQuery("hello"))
        assertEquals(PingQuery("hello"), capturedRequest)
    }

    @Test
    fun `multiple POST behaviors run in descending order`() = runTest {
        val order = mutableListOf<String>()
        val first = behavior(order = 1) { _, _, next ->
            val r = next(); order += "first"; r
        }
        val second = behavior(order = 2) { _, _, next ->
            val r = next(); order += "second"; r
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(second, first)
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("second", "first"), order)
    }

    @Test
    fun `POST behavior does not run when handler throws unhandled exception`() = runTest {
        var postRan = false
        val post = behavior { _, _, next ->
            val r = next(); postRan = true; r
        }
        val m = mediatorK {
            handle<PingQuery, String> { throw RuntimeException("boom") }
            behaviors(post)
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertFalse(postRan)
    }

    @Test
    fun `POST behavior can read context values written by PRE behavior`() = runTest {
        var postSawValue: String? = null
        val pre = behavior { _, context, next ->
            context.put("shared", "value"); next()
        }
        val post = behavior { _, context, next ->
            val r = next(); postSawValue = context.getMetaData("shared"); r
        }
        val m = mediatorK {
            handle<PingQuery, String> { "pong:${it.value}" }
            behaviors(pre, post)
        }
        m.send(PingQuery("x"))
        assertEquals("value", postSawValue)
    }
}
