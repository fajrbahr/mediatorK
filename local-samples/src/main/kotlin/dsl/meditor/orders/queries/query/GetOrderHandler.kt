package dsl.meditor.orders.queries.getorder

import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.mediatorRegistrar
import dsl.meditor.orders.queries.query.GetOrderQuery
import dsl.meditor.orders.queries.query.OrderDetails

val getOrderHandler = handler<GetOrderQuery, OrderDetails> { request ->
    OrderDetails(
        orderId = request.orderId,
        customerId = request.customerId,
        status = "CONFIRMED",
        totalAmount = 99.99,
    )
}

val getOrderRegistrar = mediatorRegistrar {
    register(getOrderHandler)
}
