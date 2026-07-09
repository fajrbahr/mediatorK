package com.fajrbahr.mediatork.test

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StubMediatorTest {

    private val mediator = StubMediator()

    @Test
    fun `returns stubbed value`() = runTest {
        mediator.on<GetUserQuery>() returns "user:42"
        assertEquals("user:42", mediator.send(GetUserQuery("42")))
    }

    @Test
    fun `throws stubbed error`() = runTest {
        mediator.on<GetUserQuery>() throws IllegalStateException("not found")
        assertFailsWith<IllegalStateException> {
            mediator.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `answers with dynamic response`() = runTest {
        mediator.on<GetUserQuery>() answers { "user:${it.id}" }
        assertEquals("user:99", mediator.send(GetUserQuery("99")))
    }

    @Test
    fun `throws when no stub registered`() = runTest {
        assertFailsWith<IllegalStateException> {
            mediator.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `supports multiple stubs`() = runTest {
        mediator.on<GetUserQuery>() returns "user"
        mediator.on<CreateOrderCommand>() returns "order"
        assertEquals("user", mediator.send(GetUserQuery("1")))
        assertEquals("order", mediator.send(CreateOrderCommand("1")))
    }

    @Test
    fun `supports Unit requests`() = runTest {
        mediator.on<DeleteOrderCommand>() returns Unit
        mediator.send(DeleteOrderCommand("1"))
    }
}
