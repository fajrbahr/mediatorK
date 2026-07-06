package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.StreamRequestHandler
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertTrue

class MediatorVerifierTest {

    @Test
    fun `verify on empty registry does not invoke callback`() {
        val registry = HandlerRegistry()
        val missed = mutableListOf<String>()
        registry.verify { missed += it }
        assertTrue(missed.isEmpty())
    }

    @Test
    fun `verify with registered request handler does not invoke callback`() {
        val registry = HandlerRegistry()
        registry register PingHandler()
        registry register AddHandler()
        val missed = mutableListOf<String>()
        registry.verify { missed += it }
        assertTrue(missed.isEmpty())
    }

    @Test
    fun `verify with registered stream handler does not invoke callback`() {
        val registry = HandlerRegistry()
        registry registerStream StreamRequestHandler<NumbersStreamQuery, Int> { mediator, requestContext, request -> emptyFlow() }
        val missed = mutableListOf<String>()
        registry.verify { missed += it }
        assertTrue(missed.isEmpty())
    }

    @Test
    fun `verify with registered notification handler does not invoke callback`() {
        val registry = HandlerRegistry()
        registry registerNotification RecordingNotificationHandler()
        val missed = mutableListOf<String>()
        registry.verify { missed += it }
        assertTrue(missed.isEmpty())
    }

    @Test
    fun `verify with all handler types registered does not invoke callback`() {
        val registry = HandlerRegistry()
        registry register PingHandler()
        registry registerStream StreamRequestHandler<NumbersStreamQuery, Int> { mediator, requestContext, request -> emptyFlow() }
        registry registerNotification RecordingNotificationHandler()
        val missed = mutableListOf<String>()
        registry.verify { missed += it }
        assertTrue(missed.isEmpty())
    }

    @Test
    fun `verify does not throw when called without callback`() {
        val registry = HandlerRegistry()
        registry register PingHandler()
        registry.verify()
    }

    private data class NumbersStreamQuery(val n: Int) : StreamRequest<Int>
}
