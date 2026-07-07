package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HandlerTestHarnessTest {

    @Test
    fun `send dispatches request and returns result`() = runTest {
        val harness = buildHandlerTestHarness {
            register(GetUserHandler())
        }
        assertEquals("user:42", harness.send(GetUserQuery("42")))
    }

    @Test
    fun `query dispatches request and returns result`() = runTest {
        val harness = buildHandlerTestHarness {
            register(GetUserHandler())
        }
        assertEquals("user:99", harness.query(GetUserQuery("99")))
    }

    @Test
    fun `given executes setup requests silently`() = runTest {
        val handler = DeleteOrderHandler()
        val harness = buildHandlerTestHarness {
            register(handler)
        }
        harness.given(DeleteOrderCommand("setup-1"), DeleteOrderCommand("setup-2"))
        assertEquals("setup-2", handler.lastId)
    }

    @Test
    fun `given followed by send and query works end-to-end`() = runTest {
        val store = mutableMapOf<String, String>()
        val createHandler = fakeHandler<CreateOrderCommand, String> { _, _, req ->
            store[req.id] = "created"
            req.id
        }
        val getHandler = fakeHandler<GetUserQuery, String> { _, _, req ->
            store[req.id] ?: "not found"
        }
        val harness = buildHandlerTestHarness {
            register(createHandler)
            register(getHandler)
        }
        harness.given(CreateOrderCommand("ORD-1"))
        val result = harness.query(GetUserQuery("ORD-1"))
        assertEquals("created", result)
    }

    @Test
    fun `stream returns flow from registered handler`() = runTest {
        val harness = buildHandlerTestHarness {
            registerStream(StreamItemsHandler())
        }
        val items = harness.stream(StreamItemsQuery("data")).toList()
        assertEquals(listOf("data-1", "data-2", "data-3"), items)
    }

    @Test
    fun `pipeline behaviors are applied`() = runTest {
        val log = mutableListOf<String>()
        val behavior = object : PipelineBehavior {
            override suspend fun <TRequest : Request<TResult>, TResult> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TRequest, TResult>,
                request: TRequest,
            ): TResult {
                log += "before"
                val result = next(request)
                log += "after"
                return result
            }
        }
        val harness = buildHandlerTestHarness(pipelineBehaviors = listOf(behavior)) {
            register(GetUserHandler())
        }
        harness.send(GetUserQuery("1"))
        assertEquals(listOf("before", "after"), log)
    }

    @Test
    fun `accepts registrars`() = runTest {
        val registrar = object : MediatorRegistrar {
            override fun register(registry: com.fajrbahr.mediatork.HandlerRegistry) {
                registry.register(GetUserHandler())
            }
        }
        val harness = buildHandlerTestHarness(registrars = listOf(registrar)) {}
        assertEquals("user:via-registrar", harness.send(GetUserQuery("via-registrar")))
    }

    @Test
    fun `throws when handler is missing`() = runTest {
        val harness = buildHandlerTestHarness()
        assertFailsWith<com.fajrbahr.mediatork.MissingHandlerException> {
            harness.send(GetUserQuery("1"))
        }
    }

    @Test
    fun `registrars can provide multiple handlers`() = runTest {
        val userRegistrar = object : MediatorRegistrar {
            override fun register(registry: com.fajrbahr.mediatork.HandlerRegistry) {
                registry.register(GetUserHandler())
            }
        }
        val orderRegistrar = object : MediatorRegistrar {
            override fun register(registry: com.fajrbahr.mediatork.HandlerRegistry) {
                registry.register(CreateOrderHandler())
            }
        }
        val harness = buildHandlerTestHarness(registrars = listOf(userRegistrar, orderRegistrar))
        assertEquals("user:1", harness.send(GetUserQuery("1")))
        assertEquals("order:ORD-1", harness.send(CreateOrderCommand("ORD-1")))
    }
}
