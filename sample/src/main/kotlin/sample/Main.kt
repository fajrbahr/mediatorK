package sample

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.ParallelNotificationPublisher
import com.fajrbahr.mediatork.SequentialNotificationPublisher
import com.fajrbahr.mediatork.validator.RequestValidator
import sample.behaviors.*
import sample.command.CreateOrderCommand
import sample.command.OrderRegistrar
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

    println("\ndone.")
}
