package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class StreamRequestTest {

    private data class NumbersQuery(val count: Int) : StreamRequest<Int>

    @Test
    fun `stream dispatches to handler and emits all items`() = runTest {
        val m = mediatorK {
            handleStream<NumbersQuery, Int> { (1..it.count).asFlow() }
        }
        assertEquals(listOf(1, 2, 3), m.stream(NumbersQuery(3)).toList())
    }

    @Test
    fun `stream with empty flow emits no items`() = runTest {
        val m = mediatorK {
            handleStream<NumbersQuery, Int> { emptyFlow() }
        }
        assertTrue(m.stream(NumbersQuery(0)).toList().isEmpty())
    }

    @Test
    fun `stream throws MissingStreamHandlerException when no handler registered`() {
        val m = mediatorK { }
        assertFailsWith<MissingStreamHandlerException> {
            m.stream(NumbersQuery(1))
        }
    }

    @Test
    fun `MissingStreamHandlerException message contains request type name`() {
        val m = mediatorK { }
        val ex = assertFailsWith<MissingStreamHandlerException> {
            m.stream(NumbersQuery(1))
        }
        assertTrue(ex.message!!.contains("NumbersQuery"))
    }

    @Test
    fun `stream pipeline behavior is invoked`() = runTest {
        var called = false
        val trackingBehavior = streamBehavior { _, _, next ->
            called = true
            next()
        }
        val m = mediatorK {
            handleStream<NumbersQuery, Int> { (1..2).asFlow() }
            streamBehaviors(trackingBehavior)
        }
        m.stream(NumbersQuery(2)).toList()
        assertTrue(called)
    }

    @Test
    fun `stream pipeline behavior with isEnabled false is skipped`() = runTest {
        var called = false
        val disabledBehavior = streamBehavior(isEnabled = false) { _, _, next ->
            called = true
            next()
        }
        val m = mediatorK {
            handleStream<NumbersQuery, Int> { (1..2).asFlow() }
            streamBehaviors(disabledBehavior)
        }
        m.stream(NumbersQuery(2)).toList()
        assertFalse(called)
    }

    @Test
    fun `stream pipeline behavior with appliesTo false is skipped`() = runTest {
        var called = false
        val selectiveBehavior = streamBehavior(appliesTo = { false }) { _, _, next ->
            called = true
            next()
        }
        val m = mediatorK {
            handleStream<NumbersQuery, Int> { (1..2).asFlow() }
            streamBehaviors(selectiveBehavior)
        }
        m.stream(NumbersQuery(2)).toList()
        assertFalse(called)
    }

    @Test
    fun `multiple stream behaviors compose outermost-first by order`() = runTest {
        val executionOrder = mutableListOf<Int>()

        fun trackingBehavior(id: Int, orderVal: Int) = streamBehavior(order = orderVal) { _, _, next ->
            executionOrder += id
            next()
        }

        val m = mediatorK {
            handleStream<NumbersQuery, Int> { (1..1).asFlow() }
            streamBehaviors(trackingBehavior(id = 2, orderVal = 20), trackingBehavior(id = 1, orderVal = 10))
        }
        m.stream(NumbersQuery(1)).toList()
        assertEquals(listOf(1, 2), executionOrder)
    }

    @Test
    fun `stream behavior can transform flow items`() = runTest {
        @Suppress("UNCHECKED_CAST")
        val doubling = streamBehavior { _, _, next ->
            (next() as Flow<Int>).map { it * 2 } as Flow<Nothing>
        }
        val m = mediatorK {
            handleStream<NumbersQuery, Int> { (1..3).asFlow() }
            streamBehaviors(doubling)
        }
        assertEquals(listOf(2, 4, 6), m.stream(NumbersQuery(3)).toList())
    }

    @Test
    fun `stream behavior is invoked for each stream call`() = runTest {
        var invokeCount = 0
        val counting = streamBehavior { _, _, next ->
            invokeCount++
            next()
        }
        val m = mediatorK {
            handleStream<NumbersQuery, Int> { (1..1).asFlow() }
            streamBehaviors(counting)
        }
        m.stream(NumbersQuery(1)).toList()
        m.stream(NumbersQuery(1)).toList()
        assertEquals(2, invokeCount)
    }

    @Test
    fun `stream handler receives correct request values`() = runTest {
        var receivedCount = -1
        val m = mediatorK {
            handleStream<NumbersQuery, Int> {
                receivedCount = it.count
                (1..it.count).asFlow()
            }
        }
        m.stream(NumbersQuery(7)).toList()
        assertEquals(7, receivedCount)
    }

    @Test
    fun `MissingStreamHandlerException is a MediatorException subtype`() {
        val m = mediatorK { }
        assertFailsWith<MediatorException> {
            m.stream(NumbersQuery(1))
        }
    }
}
