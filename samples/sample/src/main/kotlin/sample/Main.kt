package sample

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.*
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.*
import sample.behaviors.*
import sample.behaviors.RetryPipelineBehavior
import sample.bookings.queries.fetchbookings.FetchBookingsQuery
import sample.bookings.queries.fetchbookings.FetchBookingsRegistrar
import sample.exceptions.ShipOrderCommand
import sample.exceptions.ShipOrderHandler
import sample.exceptions.ShipOrderRegistrar
import sample.exceptions.demoContinueOnException
import sample.fallback.FallbackRegistrar
import sample.fallback.OrderShippedNotification
import sample.orders.commands.createorder.CreateOrderCommand
import sample.orders.commands.createorder.OrderCreatedNotification
import sample.orders.commands.createorder.OrderNotificationRegistrar
import sample.orders.commands.createorder.OrderRegistrar
import sample.orders.queries.getorder.GetOrderHandler
import sample.orders.queries.getorder.GetOrderQuery
import sample.orders.queries.getorder.GetOrderRegistrar
import sample.orders.queries.getorder.OrderDetails
import sample.users.queries.fetchuser.FetchUserQuery
import sample.users.queries.fetchuser.User
import sample.users.queries.fetchuser.UserRegistrar

private val mediator = MediatorFactory.create(
    registrars = listOf(
        UserRegistrar(),
        OrderRegistrar(),
        OrderNotificationRegistrar(),
        FetchBookingsRegistrar(),
        GetOrderRegistrar(),
    ),
    pipelineBehaviors = listOf(
        AuthBehavior(),
        LocaleBehavior(),
        LoggingBehavior(),
        MeasurePipelineBehaviour(),
        RetryPipelineBehavior(maxRetries = 2),
        TracingPipelineBehavior(),
        MetricsBehavior(),
    ),
    notificationPublisher = NotificationPublishStrategy.DEFAULT,
)

class Test1Command {
    suspend fun start() {
        println("=== TEST 1: Command ===")
        val orderResult = mediator.send(CreateOrderCommand(id = "ORD-1", amount = 150.0))
        println("Order result: $orderResult")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test1Command().start() }
    }
}

class Test2QueryFetchUser {
    suspend fun start() {
        println("=== TEST 2: Query — fetch user ===")
        val user = mediator.send(FetchUserQuery(id = "USER-1", amount = 0.0))
        println("User: $user")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test2QueryFetchUser().start() }
    }
}

class Test3QueryFetchBookingValid {
    suspend fun start() {
        println("=== TEST 3: Query — fetch booking (valid) ===")
        val booking = mediator.send(FetchBookingsQuery(userEmail = "sdasd@gmail.com", bookingId = "bx_booking#3"))
        println("Booking: $booking")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test3QueryFetchBookingValid().start() }
    }
}

class Test4ValidationInvalidBooking {
    suspend fun start() {
        println("=== TEST 4: Query — fetch booking (invalid, expect validation error) ===")
        runCatching {
            mediator.send(FetchBookingsQuery(userEmail = "sdasd@", bookingId = "123"))
        }.onFailure { throwable ->
            when (throwable) {
                is ValidationException -> println("Validation error: ${throwable.message}")

                else -> println("Unexpected error: ${throwable.message}")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test4ValidationInvalidBooking().start() }
    }
}

class Test5GetOrderValid {
    suspend fun start() {
        println("=== TEST 5: GetOrder — valid (fail-fast validator) ===")
        val order = mediator.send(GetOrderQuery(orderId = "ORD-9988", customerId = "USR-42"))
        println("Order: $order")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test5GetOrderValid().start() }
    }
}

class Test6GetOrderInvalidId {
    suspend fun start() {
        println("=== TEST 6: GetOrder — invalid orderId (fail-fast stops at first broken rule) ===")
        runCatching {
            mediator.send(GetOrderQuery(orderId = "9988", customerId = "USR-42"))
        }.onFailure { throwable ->
            when (throwable) {
                is ValidationException -> println("Validation error: ${throwable.message}")

                else -> println("Unexpected error: ${throwable.message}")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test6GetOrderInvalidId().start() }
    }
}

class Test7GetOrderBothInvalid {
    suspend fun start() {
        println("=== TEST 7: GetOrder — both fields invalid (fail-fast stops after first failure) ===")
        runCatching {
            mediator.send(GetOrderQuery(orderId = "", customerId = ""))
        }.onFailure { throwable ->
            when (throwable) {
                is ValidationException -> println("Validation error: ${throwable.message}")

                else -> println("Unexpected error: ${throwable.message}")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test7GetOrderBothInvalid().start() }
    }
}

class Test8NotificationSequential {
    suspend fun start() {
        println("=== TEST 8: Notification (sequential) ===")
        mediator.publish(
            OrderCreatedNotification(
                orderId = "ORD-223",
                customerEmail = "omar@gmail.com",
                customerPhone = "+1234567890",
                totalAmount = 5.56,
            ),
            NotificationPublishStrategy.SEQUENTIAL,
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test8NotificationSequential().start() }
    }
}

class Test9ExceptionOrderNotFound {
    suspend fun start() {
        println("=== TEST 9: Fallback chain — OrderNotFoundException recovered by fallback handler ===")
        val exMediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar()),
            notificationPublisher = NotificationPublishStrategy.SEQUENTIAL,
        )
        val result = exMediator.send(ShipOrderCommand(orderId = "MISSING", warehouseId = "WH-1"))
        println("Result: $result")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test9ExceptionOrderNotFound().start() }
    }
}

class Test10ExceptionOutOfStock {
    suspend fun start() {
        println("=== TEST 10: Fallback chain — OutOfStockException recovered by fallback handler ===")
        val exMediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar()),
            notificationPublisher = NotificationPublishStrategy.SEQUENTIAL,
        )
        val result = exMediator.send(ShipOrderCommand(orderId = "ORD-42", warehouseId = "WH-EMPTY"))
        println("Result: $result")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test10ExceptionOutOfStock().start() }
    }
}

class Test11AggregateException {
    suspend fun start() {
        println("=== TEST 11: AggregateException — NotificationPublishStrategy.CONTINUE_ON_EXCEPTION ===")
        val failingMediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar(pushFails = true, analyticsFails = true)),
            notificationPublisher = NotificationPublishStrategy.CONTINUE_ON_EXCEPTION,
        )
        demoContinueOnException(failingMediator)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test11AggregateException().start() }
    }
}

class Test12UnhandledException {
    suspend fun start() {
        println("=== TEST 12: Unhandled exception propagates as-is ===")
        runCatching {
            val noFallbackMediator = MediatorFactory.create(
                registrars = listOf(object : MediatorRegistrar {
                    override fun register(registry: HandlerRegistry) {
                        registry.register(ShipOrderHandler())
                    }
                }),
            )
            noFallbackMediator.send(ShipOrderCommand(orderId = "MISSING", warehouseId = "WH-1"))
        }.onFailure { throwable ->
            println("Unhandled ${throwable::class.simpleName}: ${throwable.message}")
            println("(No fallback handler registered — exception propagates to caller)")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test12UnhandledException().start() }
    }
}

class Test13RequestFallback {
    suspend fun start() {
        println("=== TEST 13: Request fallback chain — live API down, served from cache ===")
        val fallbackMediator = MediatorFactory.create(registrars = listOf(FallbackRegistrar()))
        val result = fallbackMediator.send(CreateOrderCommand(id = "ORD-FB-1", amount = 99.0))
        println("Result: $result")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test13RequestFallback().start() }
    }
}

class Test14NotificationFallback {
    suspend fun start() {
        println("=== TEST 14: Notification fallback chain — push down, falls back to email ===")
        val fallbackMediator = MediatorFactory.create(registrars = listOf(FallbackRegistrar()))
        fallbackMediator.publish(OrderShippedNotification(orderId = "ORD-FB-1", userId = "USR-1"))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test14NotificationFallback().start() }
    }
}

class Test15Timing {
    suspend fun start() {
        println("=== TEST 15: TimingPipelineBehavior — measures every request ===")
        val timingMediator = MediatorFactory.create(
            registrars = listOf(GetOrderRegistrar()),
            pipelineBehaviors = listOf(
                TimingPipelineBehavior { name, ms -> println("  [$name] took ${ms}ms") },
            ),
        )
        timingMediator.send(GetOrderQuery(orderId = "ORD-T1", customerId = "USR-1"))
        timingMediator.send(GetOrderQuery(orderId = "ORD-T2", customerId = "USR-2"))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test15Timing().start() }
    }
}

class Test16Caching {
    suspend fun start() {
        println("=== TEST 16: CachingPipelineBehavior — handler runs once, second call hits cache ===")
        var handlerCalls = 0
        val cache = CachingPipelineBehavior(ttlMs = 5_000)
        val cachingMediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register GetOrderHandler().let { orig ->
                        object : RequestHandler<GetOrderQuery, OrderDetails> {
                            override suspend fun handle(
                                mediator: Mediator,
                                requestContext: RequestContext,
                                request: GetOrderQuery,
                            ): OrderDetails {
                                handlerCalls++
                                println("  Handler invoked (call #$handlerCalls)")
                                return orig.handle(mediator, requestContext, request)
                            }
                        }
                    }
                }
            }),
            pipelineBehaviors = listOf(cache),
        )
        val q = GetOrderQuery(orderId = "ORD-C1", customerId = "USR-1")
        cachingMediator.send(q)
        cachingMediator.send(q) // cache hit — handler not called again
        println("  Handler called $handlerCalls time(s) for 2 sends (cache size: ${cache.size()})")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test16Caching().start() }
    }
}

class Test17Deduplication {
    suspend fun start() {
        println("=== TEST 17: DeduplicationPipelineBehavior — concurrent duplicates share one result ===")
        var handlerCalls = 0
        val dedup = DeduplicationPipelineBehavior()
        val dedupMediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object :
                        RequestHandler<GetOrderQuery, OrderDetails> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: GetOrderQuery,
                        ): OrderDetails {
                            handlerCalls++
                            delay(50) // simulate async work so the second call overlaps
                            return OrderDetails(request.orderId, request.customerId, "OK", 0.0)
                        }
                    }
                }
            }),
            pipelineBehaviors = listOf(dedup),
        )
        val q = GetOrderQuery(orderId = "ORD-D1", customerId = "USR-1")
        val results = coroutineScope {
            listOf(
                async { dedupMediator.send(q) },
                async { dedupMediator.send(q) },
                async { dedupMediator.send(q) },
            ).map { it.await() }
        }
        println("  3 concurrent sends → handler called $handlerCalls time(s), all got: ${results.map { it.orderId }}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test17Deduplication().start() }
    }
}

class Test18CircuitBreaker {
    suspend fun start() {
        println("=== TEST 18: CircuitBreakerPipelineBehavior — trips after 3 failures ===")
        val breaker = CircuitBreakerPipelineBehavior(
            failureThreshold = 3,
            resetTimeoutMs = 500,
            onStateChange = { state -> println("  Circuit → $state") },
        )
        val cbMediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                var callCount = 0
                override fun register(registry: HandlerRegistry) {
                    registry register object :
                        RequestHandler<GetOrderQuery, OrderDetails> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: GetOrderQuery,
                        ): OrderDetails {
                            callCount++
                            if (callCount <= 3) throw RuntimeException("Downstream failure #$callCount")
                            return OrderDetails(request.orderId, request.customerId, "OK", 0.0)
                        }
                    }
                }
            }),
            pipelineBehaviors = listOf(breaker),
        )
        val q = GetOrderQuery(orderId = "ORD-CB", customerId = "USR-1")
        repeat(5) { i ->
            runCatching { cbMediator.send(q) }
                .onSuccess { println("  Call ${i + 1}: success — ${it.status}") }
                .onFailure { println("  Call ${i + 1}: ${it::class.simpleName} — ${it.message}") }
        }
        println("  Waiting for reset timeout...")
        delay(600)
        runCatching { cbMediator.send(q) }
            .onSuccess { println("  Probe after reset: success — ${it.status}") }
            .onFailure { println("  Probe after reset: ${it::class.simpleName}") }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test18CircuitBreaker().start() }
    }
}

class Test19RateLimit {
    suspend fun start() {
        println("=== TEST 19: RateLimitPipelineBehavior — max 3 per 500 ms ===")
        val rateLimitedMediator = MediatorFactory.create(
            registrars = listOf(GetOrderRegistrar()),
            pipelineBehaviors = listOf(
                RateLimitPipelineBehavior(maxRequests = 3, windowMs = 500),
            ),
        )
        val q = GetOrderQuery(orderId = "ORD-RL", customerId = "USR-1")
        repeat(5) { i ->
            runCatching { rateLimitedMediator.send(q) }
                .onSuccess { println("  Send ${i + 1}: OK") }
                .onFailure { println("  Send ${i + 1}: ${it::class.simpleName} — ${it.message}") }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test19RateLimit().start() }
    }
}

class Test20ErrorTracking {
    suspend fun start() {
        println("=== TEST 20: ErrorTrackingPipelineBehavior — reports error then rethrows ===")
        val tracked = mutableListOf<String>()
        val trackingMediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object :
                        RequestHandler<GetOrderQuery, OrderDetails> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: GetOrderQuery,
                        ): OrderDetails = throw RuntimeException("Simulated crash")
                    }
                }
            }),
            pipelineBehaviors = listOf(
                ErrorTrackingPipelineBehavior { request, error ->
                    val entry = "TRACKED: ${request::class.simpleName} → ${error.message}"
                    tracked += entry
                    println("  $entry")
                },
            ),
        )
        runCatching { trackingMediator.send(GetOrderQuery(orderId = "ORD-ET", customerId = "USR-1")) }
            .onFailure { println("  Caller received: ${it.message}") }
        println("  Errors tracked: ${tracked.size}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test20ErrorTracking().start() }
    }
}

class Test21RequestCounter {
    suspend fun start() {
        println("=== TEST 21: RequestCounterPipelineBehavior — counts dispatches per type ===")
        val counter = RequestCounterPipelineBehavior()
        val countingMediator = MediatorFactory.create(
            registrars = listOf(GetOrderRegistrar(), FetchBookingsRegistrar(), UserRegistrar()),
            pipelineBehaviors = listOf(counter),
        )
        repeat(3) { countingMediator.send(GetOrderQuery(orderId = "ORD-$it", customerId = "USR-1")) }
        countingMediator.send(FetchUserQuery(id = "USR-1", amount = 0.0))
        println("  GetOrderQuery:  ${counter.countFor(GetOrderQuery::class)} dispatches")
        println("  FetchUserQuery: ${counter.countFor(FetchUserQuery::class)} dispatches")
        println("  Snapshot: ${counter.snapshot()}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test21RequestCounter().start() }
    }
}

/** Marker so only GetOrderQuery is subject to auth checks. */
data class AuthenticatedGetOrderQuery(val orderId: String, val customerId: String) :
    Request<OrderDetails>, AuthenticatedRequest

class Test22Authorization {
    suspend fun start() {
        println("=== TEST 22: AuthorizationPipelineBehavior — passes with token, rejects without ===")
        val authMediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object :
                        RequestHandler<AuthenticatedGetOrderQuery, OrderDetails> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: AuthenticatedGetOrderQuery,
                        ) = OrderDetails(request.orderId, request.customerId, "CONFIRMED", 99.0)
                    }
                }
            }),
            pipelineBehaviors = listOf(
                object : PipelineBehavior {
                    override val stage = Stage.Pre
                    override suspend fun <TRequest : Request<TResult>, TResult> process(
                        requestContext: RequestContext,
                        next: RequestHandlerDelegate<TRequest, TResult>,
                        request: TRequest,
                    ): TResult {
                        // Inject a valid token only for specific test IDs
                        if (request is AuthenticatedGetOrderQuery && request.orderId == "ORD-AUTH") {
                            requestContext.put("token", "secret")
                        }
                        return next(request)
                    }
                },
                AuthorizationPipelineBehavior { context, _ ->
                    val token = context.getMetaDate<String>("token")
                        ?: throw UnauthorizedException("No token in context")
                    if (token != "secret") throw UnauthorizedException("Invalid token")
                },
            ),
        )

        // Authorized
        runCatching {
            authMediator.send(AuthenticatedGetOrderQuery(orderId = "ORD-AUTH", customerId = "USR-1"))
        }.onSuccess { println("  Authorized  → ${it.status}") }
            .onFailure { println("  Authorized  → FAILED: ${it.message}") }

        // Unauthorized (no token injected)
        runCatching {
            authMediator.send(AuthenticatedGetOrderQuery(orderId = "ORD-NOAUTH", customerId = "USR-2"))
        }.onSuccess { println("  Unauthorized → OK (unexpected)") }
            .onFailure { println("  Unauthorized → ${it::class.simpleName}: ${it.message}") }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test22Authorization().start() }
    }
}

class Test23FireAndForget {
    suspend fun start() {
        println("=== TEST 23: NotificationPublishStrategy.fireAndForget — returns immediately, handler runs in background ===")
        val received = mutableListOf<String>()
        val ffMediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry.registerNotification(object :
                        NotificationHandler<OrderCreatedNotification> {
                        override suspend fun handle(notification: OrderCreatedNotification) {
                            delay(50) // simulate async work
                            received += notification.orderId
                            println("  Background handler processed: ${notification.orderId}")
                        }
                    })
                }
            }),
            notificationPublisher = NotificationPublishStrategy.fireAndForget(GlobalScope),
        )
        ffMediator.publish(OrderCreatedNotification("ORD-FF-1", "a@b.com", "+1", 10.0))
        println("  publish() returned immediately — handler still running")
        delay(200) // let the background coroutine finish
        println("  After wait — received: $received")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test23FireAndForget().start() }
    }
}

class Test24SilentMissingNotification {
    suspend fun start() {
        println("=== TEST 24: SilentMissingNotificationHandler — no handler registered, no exception thrown ===")
        val silentMediator = MediatorFactory.create(
            registrars = emptyList(),
            missingNotificationHandler = SilentMissingNotificationHandler(),
        )
        silentMediator.publish(OrderCreatedNotification("ORD-SILENT", "a@b.com", "+1", 0.0))
        println("  Published with no handlers registered — no exception (silent drop)")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test24SilentMissingNotification().start() }
    }
}
