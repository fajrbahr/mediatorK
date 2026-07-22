package sample.meditor

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.mediatorK
import sample.meditor.behaviors.localeBehavior
import sample.meditor.behaviors.measureBehavior
import sample.meditor.orders.create.createOrderHandler
import sample.meditor.orders.create.sendOrderConfirmationEmailHandler
import sample.meditor.orders.create.sendOrderSmsHandler
import sample.meditor.orders.queries.query.getOrderHandler
import sample.meditor.orders.queries.query.getOrderValidator

/** Builds the demo mediator with the full FP DSL: behaviors, handlers, a validator, and notifications. */
fun orderMediator(): Mediator = mediatorK {
    behaviors(localeBehavior(), measureBehavior())

    handle(createOrderHandler)
    handle(getOrderHandler)
    validate(getOrderValidator)

    notification(notificationHandler = sendOrderConfirmationEmailHandler)
    notification(notificationHandler = sendOrderSmsHandler)
}
