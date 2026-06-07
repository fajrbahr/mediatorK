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
import sample.query.FetchUserHandlerRegistrar
import sample.query.FetchUserQuery
import sample.query.FetchUserQueryId
import sample.query.UserRegistrar
import sample.validation.FetchBookingsByEmailQueryValidator
import sample.validation.FetchBookingsByEmailQueryValidatorF
import sample.validation.FetchBookingsByEmailQueryValidatorField

val validators: List<RequestValidator<*>> = listOf(
    FetchBookingsByEmailQueryValidatorF(),
    // ... other validators
)
val mediator = MediatorFactory.create(
    registrars = listOf(
        UserRegistrar(), OrderRegistrar(), OrderNotificationRegistrar(), FetchUserHandlerRegistrar()
    ), pipelineBehaviors = listOf(
        LoggingBehavior(),
        MeasurePipelineBehaviour(),
        RetryPipelineBehavior(maxRetries = 2),
        TracingPipelineBehavior(),
        ValidationBehavior(validators),
    ), preProcessors = listOf(
        AuthPreProcessor(), LocalePreProcessor()
    ), postProcessors = listOf(
        MetricsPostProcessor()
    ), notificationPublisher = ParallelNotificationPublisher()
)


suspend fun main() {
    // 🔹 TEST 1: Command
    val orderResult = mediator.send(
        CreateOrderCommand(
            id = "ORD-1", amount = 150.0
        )
    )

    // 🔹 TEST 2: Query
    val user = mediator.send(
        FetchUserQuery(
            id = "USER-1", amount = 0.0
        )
    )

    // 🔹 TEST 3: Query
    val booking = mediator.send(
        FetchUserQueryId(
            userEmail = "sdasd@gmail.com", bookingId = "bx_booking#3"
        )
    )

    runCatching {
        mediator.send(
            FetchUserQueryId(
                userEmail = "sdasd@", bookingId = "123"
            )
        )
    }.onFailure { throwable ->
        when (throwable) {
            is ValidationException -> {
                val fieldErrors = throwable.errors.mapNotNull { error ->
                    when (error.field) {
                        is FetchBookingsByEmailQueryValidatorField.BookingId -> "M_Booking ID error: ${error.message}"

                        is FetchBookingsByEmailQueryValidatorField.UserEmail -> "M_Email error: ${error.message}"

                        else -> null // ignore unknown fields or DefaultField
                    }
                }
                fieldErrors.forEach {
                    println("$it")
                }
            }

            else -> println("Unexpected error: ${throwable.message}")
        }
    }


//    // Publish notification trigger
//    // UpdateInventoryHandler
//    // SendOrderSmsHandler
//    // SendOrderConfirmationEmailHandler
//    // TrackOrderAnalyticsHandler
//
    mediator.publish(
        OrderCreatedNotification(
            orderId = "id_223", customerEmail = "omar@gmiasl.com", customerPhone = "+1234567890", totalAmount = 5.56
        ), SequentialNotificationPublisher()
    )

    println("done.....")
}