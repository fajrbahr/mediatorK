package sample

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.notification.ContinueOnExceptionNotificationPublisher
import com.fajrbahr.mediatork.notification.ParallelNotificationPublisher
import com.fajrbahr.mediatork.notification.SequentialNotificationPublisher
import com.fajrbahr.mediatork.validator.RequestValidator
import kotlinx.coroutines.runBlocking
import sample.behaviors.*
import sample.command.CreateOrderCommand
import sample.command.OrderRegistrar
import sample.exceptions.ShipOrderCommand
import sample.exceptions.ShipOrderHandler
import sample.exceptions.ShipOrderRegistrar
import sample.exceptions.demoContinueOnException
import sample.fallback.FallbackRegistrar
import sample.fallback.OrderShippedNotification
import sample.notification.OrderCreatedNotification
import sample.notification.OrderNotificationRegistrar
import sample.query.*
import sample.validation.FetchBookingsByEmailQueryValidator
import sample.validation.FetchBookingsByEmailQueryValidatorField
import sample.validation.GetOrderField
import sample.validation.GetOrderQueryValidator

private val validators: List<RequestValidator<*>> = listOf(
    FetchBookingsByEmailQueryValidator(),
    GetOrderQueryValidator(),
)

private val mediator = MediatorFactory.create(
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
        val booking = mediator.send(FetchUserQueryId(userEmail = "sdasd@gmail.com", bookingId = "bx_booking#3"))
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
            mediator.send(FetchUserQueryId(userEmail = "sdasd@", bookingId = "123"))
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
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test6GetOrderInvalidId().start() }
    }
}

class Test7GetOrderBothInvalid {
    suspend fun start() {
        println("=== TEST 7: GetOrder — both fields invalid (fail-fast stops after first ruleFor fails) ===")
        runCatching {
            mediator.send(GetOrderQuery(orderId = "", customerId = ""))
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
            SequentialNotificationPublisher(),
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking { Test8NotificationSequential().start() }
    }
}

class Test9ExceptionOrderNotFound {
    suspend fun start() {
        println("=== TEST 9: RequestExceptionHandler — OrderNotFoundException recovered ===")
        val exMediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar()),
            notificationPublisher = SequentialNotificationPublisher(),
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
        println("=== TEST 10: RequestExceptionHandler — OutOfStockException recovered ===")
        val exMediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar()),
            notificationPublisher = SequentialNotificationPublisher(),
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
        println("=== TEST 11: AggregateException — ContinueOnExceptionNotificationPublisher ===")
        val failingMediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar(pushFails = true, analyticsFails = true)),
            notificationPublisher = ContinueOnExceptionNotificationPublisher(),
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
            val noExHandlerMediator = MediatorFactory.create(
                registrars = listOf(object : MediatorRegistrar {
                    override fun register(registry: HandlerRegistry) {
                        registry.register(ShipOrderHandler())
                    }
                }),
            )
            noExHandlerMediator.send(ShipOrderCommand(orderId = "MISSING", warehouseId = "WH-1"))
        }.onFailure { throwable ->
            println("Unhandled ${throwable::class.simpleName}: ${throwable.message}")
            println("(No RequestExceptionHandler registered — exception propagates to caller)")
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
