package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.validator.ValidationResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HandlerRegistryTest {

    // ── register / resolve ─────────────────────────────────────────────────────

    @Test
    fun `register adds handler that can be resolved`() = runTest {
        val registry = HandlerRegistry()
        registry.register(PingHandler())
        val request = PingQuery("x")
        val handler = registry.resolveHandler(request)
        assertEquals("pong:x", handler.handle(object : Mediator {
            override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult = TODO()
            override suspend fun <T : Notification> publish(notification: T) = TODO()
            override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) =
                TODO()

            override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): kotlinx.coroutines.flow.Flow<T> =
                TODO()
        }, RequestContext(), request))
    }

    @Test
    fun `hasHandler returns true after registration`() {
        val registry = HandlerRegistry()
        registry.register(PingHandler())
        assertTrue(registry.hasHandler(PingQuery::class))
    }

    @Test
    fun `hasHandler returns false before registration`() {
        val registry = HandlerRegistry()
        assertFalse(registry.hasHandler(PingQuery::class))
    }

    @Test
    fun `register replaces previously registered handler for same type`() = runTest {
        val registry = HandlerRegistry()
        val first = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) =
                "first"
        }
        val second = object : RequestHandler<PingQuery, String> {
            override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery) =
                "second"
        }
        registry.register(first)
        registry.register(second)
        val m = MediatorFactory.create(registry = HandlerRegistry().apply {
            register(second)
        })
        assertEquals("second", m.send(PingQuery("x")))
    }

    @Test
    fun `resolveHandler throws MissingHandlerException when absent`() {
        val registry = HandlerRegistry()
        assertFailsWith<MissingHandlerException> { registry.resolveHandler(PingQuery("x")) }
    }

    // ── notification handlers ─────────────────────────────────────────────────

    @Test
    fun `registerNotification adds handler`() {
        val registry = HandlerRegistry()
        val h = RecordingNotificationHandler()
        registry.registerNotification(h)
        val resolved = registry.resolveNotificationHandlers(PingNotification("x"))
        assertTrue(resolved.contains(h))
    }

    @Test
    fun `registerNotification allows multiple handlers for same type`() {
        val registry = HandlerRegistry()
        val h1 = RecordingNotificationHandler()
        val h2 = RecordingNotificationHandler()
        registry.registerNotification(h1)
        registry.registerNotification(h2)
        val resolved = registry.resolveNotificationHandlers(PingNotification("x"))
        assertEquals(2, resolved.size)
    }

    @Test
    fun `resolveNotificationHandlers returns empty list when none registered`() {
        val registry = HandlerRegistry()
        val resolved = registry.resolveNotificationHandlers(PingNotification("x"))
        assertTrue(resolved.isEmpty())
    }

    // ── DSL operators ─────────────────────────────────────────────────────────

    @Test
    fun `unaryPlus registers request handler`() = runTest {
        val m = MediatorFactory.create(registry = HandlerRegistry().apply {
            with(this) { +PingHandler() }
        })
        assertEquals("pong:y", m.send(PingQuery("y")))
    }

    @Test
    fun `unaryPlus registers notification handler`() = runTest {
        val h = RecordingNotificationHandler()
        val m = MediatorFactory.create(registry = HandlerRegistry().apply {
            with(this) { +h }
        })
        m.publish(PingNotification("hello"))
        assertEquals(listOf("hello"), h.received)
    }

    @Test
    fun `scope block groups registrations`() = runTest {
        val m = MediatorFactory.create(registry = HandlerRegistry().apply {
            scope {
                register(PingHandler())
                register(AddHandler())
            }
        })
        assertEquals("pong:z", m.send(PingQuery("z")))
        assertEquals(5, m.send(AddCommand(2, 3)))
    }

    // ── chaining ──────────────────────────────────────────────────────────────

    @Test
    fun `register returns registry for chaining`() {
        val registry = HandlerRegistry()
        val returned = registry.register(PingHandler())
        assertSame(registry, returned)
    }

    @Test
    fun `registerNotification returns registry for chaining`() {
        val registry = HandlerRegistry()
        val h = RecordingNotificationHandler()
        val returned = registry.registerNotification(h)
        assertSame(registry, returned)
    }

    // ── registerValidator ─────────────────────────────────────────────────────

    @Test
    fun `registerValidator stores validator for request type`() = runTest {
        val registry = HandlerRegistry()
        val validator = object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult = ValidationResult.Valid
        }
        registry.registerValidator(validator)
        assertTrue(registry.anyValidators().containsKey(PingQuery::class))
        assertEquals(1, registry.anyValidators()[PingQuery::class]?.size)
    }

    @Test
    fun `registerValidator appends multiple validators for same type`() = runTest {
        val registry = HandlerRegistry()
        val v1 = object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult = ValidationResult.Valid
        }
        val v2 = object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult = ValidationResult.Valid
        }
        registry.registerValidator(v1)
        registry.registerValidator(v2)
        assertEquals(2, registry.anyValidators()[PingQuery::class]?.size)
    }

    @Test
    fun `unaryPlus registers validator`() = runTest {
        val registry = HandlerRegistry()
        val validator = object : RequestValidator<PingQuery> {
            override fun validate(request: PingQuery): ValidationResult = ValidationResult.Valid
        }
        with(registry) { +validator }
        assertTrue(registry.anyValidators().containsKey(PingQuery::class))
    }

    @Test
    fun `unaryPlus registers stream handler`() = runTest {
        val registry = HandlerRegistry()
        val handler = object : StreamRequestHandler<StreamNumbersQuery, Int> {
            override fun handle(
                mediator: com.fajrbahr.mediatork.api.Mediator,
                requestContext: RequestContext,
                request: StreamNumbersQuery,
            ): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
        }
        with(registry) { +handler }
        assertTrue(registry.hasStreamHandler(StreamNumbersQuery::class))
    }

    // ── Dynamic registration ──────────────────────────────────────────────────

    @Test
    fun `registerDynamic registers handler by KClass`() = runTest {
        val registry = HandlerRegistry()
        registry.registerDynamic(PingQuery::class, PingHandler())
        assertTrue(registry.hasHandler(PingQuery::class))
    }

    @Test
    fun `registerDynamic returns registry for chaining`() {
        val registry = HandlerRegistry()
        val returned = registry.registerDynamic(PingQuery::class, PingHandler())
        assertSame(registry, returned)
    }

    @Test
    fun `registerStreamDynamic registers stream handler by KClass`() {
        val registry = HandlerRegistry()
        val handler = object : StreamRequestHandler<StreamNumbersQuery, Int> {
            override fun handle(
                mediator: com.fajrbahr.mediatork.api.Mediator,
                requestContext: RequestContext,
                request: StreamNumbersQuery,
            ): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
        }
        registry.registerStreamDynamic(StreamNumbersQuery::class, handler)
        assertTrue(registry.hasStreamHandler(StreamNumbersQuery::class))
    }

    @Test
    fun `registerStreamDynamic returns registry for chaining`() {
        val registry = HandlerRegistry()
        val handler = object : StreamRequestHandler<StreamNumbersQuery, Int> {
            override fun handle(
                mediator: com.fajrbahr.mediatork.api.Mediator,
                requestContext: RequestContext,
                request: StreamNumbersQuery,
            ): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
        }
        val returned = registry.registerStreamDynamic(StreamNumbersQuery::class, handler)
        assertSame(registry, returned)
    }

    @Test
    fun `registerNotificationDynamic appends handler by KClass`() = runTest {
        val registry = HandlerRegistry()
        val h = RecordingNotificationHandler()
        registry.registerNotificationDynamic(PingNotification::class, h)
        assertEquals(1, registry.resolveNotificationHandlers(PingNotification("x")).size)
    }

    @Test
    fun `registerNotificationDynamic returns registry for chaining`() {
        val registry = HandlerRegistry()
        val returned = registry.registerNotificationDynamic(PingNotification::class, RecordingNotificationHandler())
        assertSame(registry, returned)
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Test
    fun `registeredRequestTypes returns all registered request types`() {
        val registry = HandlerRegistry()
        registry.register(PingHandler())
        registry.register(AddHandler())
        val types = registry.registeredRequestTypes()
        assertTrue(PingQuery::class in types)
        assertTrue(AddCommand::class in types)
        assertEquals(2, types.size)
    }

    @Test
    fun `registeredRequestTypes is empty when nothing registered`() {
        val registry = HandlerRegistry()
        assertTrue(registry.registeredRequestTypes().isEmpty())
    }

    @Test
    fun `registeredStreamRequestTypes returns registered stream types`() {
        val registry = HandlerRegistry()
        val handler = object : StreamRequestHandler<StreamNumbersQuery, Int> {
            override fun handle(
                mediator: com.fajrbahr.mediatork.api.Mediator,
                requestContext: RequestContext,
                request: StreamNumbersQuery,
            ): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
        }
        registry.registerStream(handler)
        val types = registry.registeredStreamRequestTypes()
        assertTrue(StreamNumbersQuery::class in types)
    }

    @Test
    fun `registeredStreamRequestTypes is empty when nothing registered`() {
        val registry = HandlerRegistry()
        assertTrue(registry.registeredStreamRequestTypes().isEmpty())
    }

    @Test
    fun `hasStreamHandler returns true after registerStreamDynamic`() {
        val registry = HandlerRegistry()
        val handler = object : StreamRequestHandler<StreamNumbersQuery, Int> {
            override fun handle(
                mediator: com.fajrbahr.mediatork.api.Mediator,
                requestContext: RequestContext,
                request: StreamNumbersQuery,
            ): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
        }
        registry.registerStreamDynamic(StreamNumbersQuery::class, handler)
        assertTrue(registry.hasStreamHandler(StreamNumbersQuery::class))
    }

    @Test
    fun `hasNotificationHandler returns true after registerNotificationDynamic`() {
        val registry = HandlerRegistry()
        registry.registerNotificationDynamic(PingNotification::class, RecordingNotificationHandler())
        assertTrue(registry.hasNotificationHandler(PingNotification::class))
    }
}

private data class StreamNumbersQuery(val n: Int) : StreamRequest<Int>
