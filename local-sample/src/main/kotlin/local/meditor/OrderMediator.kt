package local.meditor

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.mediatorK
import local.meditor.behaviors.localeBehavior
import local.meditor.behaviors.measureBehavior
import local.meditor.orders.create.createOrderHandler
import local.meditor.orders.create.sendOrderConfirmationEmailHandler
import local.meditor.orders.create.sendOrderSmsHandler
import local.meditor.orders.query.getOrderHandler
import local.meditor.orders.query.getOrderValidator

/** Builds the demo mediator with the full FP DSL: behaviors, handlers, a validator, and notifications. */
fun orderMediator(): Mediator = mediatorK {
    behaviors(localeBehavior(), measureBehavior())

    handle(createOrderHandler)
    handle(getOrderHandler)
    validate(getOrderValidator)

    notification(notificationHandler = sendOrderConfirmationEmailHandler)
    notification(notificationHandler = sendOrderSmsHandler)
}
