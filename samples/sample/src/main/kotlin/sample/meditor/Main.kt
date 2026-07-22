package sample.meditor

import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.runBlocking
import sample.meditor.orders.create.CreateOrderCommand
import sample.meditor.orders.queries.query.GetOrderQuery

private val mediator = orderMediator()

fun main(): Unit = runBlocking {

    // 1. Command — create an order
    println("=== Command: CreateOrder ===")
    val orderResult = mediator.send(
        CreateOrderCommand(id = "1", amount = 150.0)
    )
    println("Order result: $orderResult")

    println()

    // 2. Query — get order details (valid)
    println("=== Query: GetOrder (valid) ===")
    val order = mediator.send(
        GetOrderQuery(orderId = "ORD-9988", customerId = "USR-42")
    )
    println("Order: $order")

    println()

    // 3. Query — get order details (invalid, triggers validator)
    println("=== Query: GetOrder (invalid — validation error) ===")
    runCatching {
        mediator.send(
            GetOrderQuery(orderId = "9988", customerId = "USR-42")
        )
    }.onFailure { throwable ->
        when (throwable) {
            is ValidationException -> println("Validation error: ${throwable.message}")
            else -> println("Unexpected error: ${throwable.message}")
        }
    }
}
