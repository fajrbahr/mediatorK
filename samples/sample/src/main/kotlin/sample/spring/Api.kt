package sample.spring

import com.fajrbahr.mediatork.api.Mediator
import sample.orders.commands.createorder.CreateOrderCommand
import sample.orders.commands.createorder.OrderResult
import sample.users.queries.fetchuser.FetchUserQuery
import sample.users.queries.fetchuser.User

@RestController
@RequestMapping("/orders")
class OrderController(private val mediator: Mediator) {

    // Create a new order (command)
    @PostMapping
    suspend fun createOrder(@RequestBody command: CreateOrderCommand): OrderResult =
        mediator.send(command)

    // Get order by ID (query) – assuming you have a GetOrderQuery
    @GetMapping("/{id}")
    suspend fun geUser(@PathVariable id: String): User =
        mediator.send(FetchUserQuery(id, 5.5))

//    // Get all orders for a user (query)
//    @GetMapping("/user/{userId}")
//    suspend fun getUserOrders(@PathVariable userId: String): List<Order> =
//        mediator.send(GetUserOrdersQuery(userId))
//
//    // Update order status (command)
//    @PatchMapping("/{id}/status")
//    suspend fun updateOrderStatus(
//        @PathVariable id: String,
//        @RequestBody command: UpdateOrderStatusCommand
//    ): OrderResult {
//        command.orderId = id
//        return mediator.send(command)
//    }
//
//    // Delete order (command with Unit response)
//    @DeleteMapping("/{id}")
//    suspend fun deleteOrder(@PathVariable id: String): Unit =
//        mediator.send(DeleteOrderCommand(id))
}

annotation class DeleteMapping(val value: String)
annotation class RequestMapping(val value: String)
annotation class GetMapping(val value: String)
annotation class PatchMapping(val value: String)

annotation class RestController
annotation class PostMapping
annotation class PathVariable
annotation class RequestBody
