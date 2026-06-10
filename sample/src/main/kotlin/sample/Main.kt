package sample
import com.fajrbahr.mediatork.handler.*
import com.fajrbahr.mediatork.notification.*

import com.fajrbahr.mediatork.ContinueOnExceptionNotificationPublisher
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.ParallelNotificationPublisher
import com.fajrbahr.mediatork.SequentialNotificationPublisher
import com.fajrbahr.mediatork.validator.RequestValidator
import sample.behaviors.*
import sample.command.CreateOrderCommand
import sample.command.OrderRegistrar
import sample.fallback.FallbackRegistrar
import sample.fallback.OrderShippedNotification
import sample.exceptions.ShipOrderCommand
import sample.exceptions.ShipOrderRegistrar
import sample.exceptions.demoContinueOnException
import sample.notification.OrderCreatedNotification
import sample.notification.OrderNotificationRegistrar
import sample.query.*
import sample.validation.FetchBookingsByEmailQueryValidator
import sample.validation.FetchBookingsByEmailQueryValidatorField
import sample.validation.GetOrderField
import sample.validation.GetOrderQueryValidator

val validators: List<RequestValidator<*>> = listOf(
    FetchBookingsByEmailQueryValidator(),
    GetOrderQueryValidator(),
)

val mediator = MediatorFactory.create(
    registrars = listOf(
        UserRegistrar(),
        OrderRegistrar(),
        OrderNotificationRegistrar(),
        FetchUserHandlerRegistrar(),
        GetOrderRegistrar(),
    ),
    pipelineBehaviors = listOf(
        LoggingBehavior(),
        MeasurePipelineBehaviour(),
        RetryPipelineBehavior(maxRetries = 2),
        TracingPipelineBehavior(),
        ValidationBehavior(validators),
    ),
    preProcessors = listOf(
        AuthPreProcessor(),
        LocalePreProcessor(),
    ),
    postProcessors = listOf(
        MetricsPostProcessor(),
    ),
    notificationPublisher = ParallelNotificationPublisher(),
)

suspend fun main() {
    println("=== TEST 1: Command ===")
    val orderResult = mediator.send(
        CreateOrderCommand(id = "ORD-1", amount = 150.0)
    )
    println("Order result: $orderResult")

    println("\n=== TEST 2: Query — fetch user === ")
    val user = mediator.send(
        FetchUserQuery(id = "USER-1", amount = 0.0)
    )
    println("User: $user")

    println("\n=== TEST 3: Query — fetch booking (valid) ===")
    val booking = mediator.send(
        FetchUserQueryId(userEmail = "sdasd@gmail.com", bookingId = "bx_booking#3")
    )
    println("Booking: $booking")

    println("\n=== TEST 4: Query — fetch booking (invalid, expect validation error) ===")
    runCatching {
        mediator.send(
            FetchUserQueryId(userEmail = "sdasd@", bookingId = "123")
        )
    }.onFailure { throwable ->
        when (throwable) {
            is ValidationException -> throwable.errors.forEach { error ->
                when (error.field) {
                    is FetchBookingsByEmailQueryValidatorField.BookingId -> println("Booking ID error: ${error.message}")
                    is FetchBookingsByEmailQueryValidatorField.UserEmail -> println("Email error: ${error.message}")
                    else -> println("Error: ${error.message}")
                }
            }

            else -> println("Unexpected error: ${throwable.message}")
        }
    }

    println("\n=== TEST 5: GetOrder — valid (fail-fast validator) ===")
    val order = mediator.send(
        GetOrderQuery(orderId = "ORD-9988", customerId = "USR-42")
    )
    println("Order: $order")

    println("\n=== TEST 6: GetOrder — invalid orderId (fail-fast stops at first broken rule) ===")
    runCatching {
        mediator.send(
            GetOrderQuery(orderId = "9988", customerId = "USR-42")
        )
    }.onFailure { throwable ->
        when (throwable) {
            is ValidationException -> throwable.errors.forEach { error ->
                when (error.field) {
                    is GetOrderField.OrderId -> println("Order ID error: ${error.message}")
                    is GetOrderField.CustomerId -> println("Customer ID error: ${error.message}")
                    else -> println("Error: ${error.message}")
                }
            }

            else -> println("Unexpected error: ${throwable.message}")
        }
    }

    println("\n=== TEST 7: GetOrder — both fields invalid (fail-fast stops after first ruleFor fails) ===")
    runCatching {
        mediator.send(
            GetOrderQuery(orderId = "", customerId = "")
        )
    }.onFailure { throwable ->
        when (throwable) {
            is ValidationException -> throwable.errors.forEach { error ->
                when (error.field) {
                    is GetOrderField.OrderId -> println("Order ID error: ${error.message}")
                    is GetOrderField.CustomerId -> println("Customer ID error: ${error.message}")
                    else -> println("Error: ${error.message}")
                }
            }

            else -> println("Unexpected error: ${throwable.message}")
        }
    }

    println("\n=== TEST 8: Notification (sequential) ===")
    mediator.publish(
        OrderCreatedNotification(
            orderId = "ORD-223",
            customerEmail = "omar@gmail.com",
            customerPhone = "+1234567890",
            totalAmount = 5.56,
        ),
        SequentialNotificationPublisher(),
    )

    // ── Exception handling demos ───────────────────────────────────────────────

    println("\n=== TEST 9: RequestExceptionHandler — OrderNotFoundException recovered ===")
    // ShipOrderRegistrar registers two exception handlers:
    //   • OrderNotFoundExceptionHandler  → catches OrderNotFoundException
    //   • OutOfStockExceptionHandler     → catches OutOfStockException
    // Neither exception reaches the caller; each is converted to a ShipmentResult.
    val exMediator = MediatorFactory.create(
        registrars = listOf(ShipOrderRegistrar()),
        notificationPublisher = SequentialNotificationPublisher(),
    )

    val notFoundResult = exMediator.send(ShipOrderCommand(orderId = "MISSING", warehouseId = "WH-1"))
    println("Result: $notFoundResult")

    println("\n=== TEST 10: RequestExceptionHandler — OutOfStockException recovered ===")
    val outOfStockResult = exMediator.send(ShipOrderCommand(orderId = "ORD-42", warehouseId = "WH-EMPTY"))
    println("Result: $outOfStockResult")

    println("\n=== TEST 11: AggregateException — ContinueOnExceptionNotificationPublisher ===")
    // Two of the four notification handlers are configured to fail.
    // ContinueOnExceptionNotificationPublisher runs ALL handlers regardless, then
    // bundles every failure into a single AggregateException.
    val failingMediator = MediatorFactory.create(
        registrars = listOf(ShipOrderRegistrar(pushFails = true, analyticsFails = true)),
        notificationPublisher = ContinueOnExceptionNotificationPublisher(),
    )
    demoContinueOnException(failingMediator)

    println("\n=== TEST 12: Unhandled exception propagates as-is ===")
    // When no exception handler is registered for the thrown type, the exception
    // bubbles out of send() untouched — callers must handle it themselves.
    val bareMediator = MediatorFactory.create(
        registrars = listOf(ShipOrderRegistrar()),
        notificationPublisher = SequentialNotificationPublisher(),
    )
    runCatching {
        // "MISSING" triggers OrderNotFoundException; no handler registered in bareMediator
        // for this demo — we re-create without exception handlers to show propagation.
        val noExHandlerMediator = MediatorFactory.create(
            registrars = listOf(object : com.fajrbahr.mediatork.MediatorRegistrar {
                override fun register(registry: com.fajrbahr.mediatork.HandlerRegistry) {
                    registry.register(sample.exceptions.ShipOrderHandler())
                }
            }),
        )
        noExHandlerMediator.send(ShipOrderCommand(orderId = "MISSING", warehouseId = "WH-1"))
    }.onFailure { throwable ->
        println("❌ Unhandled ${throwable::class.simpleName}: ${throwable.message}")
        println("   (No RequestExceptionHandler registered — exception propagates to caller)")
    }

    // ── Fallback chain demos ───────────────────────────────────────────────────

    val fallbackMediator = MediatorFactory.create(
        registrars = listOf(FallbackRegistrar()),
    )

    println("\n=== TEST 13: Request fallback chain — live API down, served from cache ===")
    // LiveCreateOrderHandler throws → CachedCreateOrderHandler succeeds
    val fallbackOrder = fallbackMediator.send(CreateOrderCommand(id = "ORD-FB-1", amount = 99.0))
    println("Result: $fallbackOrder")

    println("\n=== TEST 14: Notification fallback chain — push down, falls back to email ===")
    // PushShippedHandler throws → EmailShippedHandler succeeds
    fallbackMediator.publish(OrderShippedNotification(orderId = "ORD-FB-1", userId = "USR-1"))

    println("\ndone.")
}
