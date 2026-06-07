package com.opentool.mediatork

import com.opentool.mediatork.com.opentool.mediatork.MediatorFactory
import com.opentool.mediatork.com.opentool.mediatork.Publisher
import com.opentool.mediatork.com.opentool.mediatork.Sender
import com.opentool.mediatork.mediator.behaviors.AuthPreProcessor
import com.opentool.mediatork.mediator.behaviors.CreateOrderCommand
import com.opentool.mediatork.mediator.behaviors.FetchUserQuery
import com.opentool.mediatork.mediator.behaviors.LoggingBehavior
import com.opentool.mediatork.mediator.behaviors.MetricsPostProcessor
import com.opentool.mediatork.mediator.behaviors.OrderRegistrar
import com.opentool.mediatork.mediator.behaviors.RetryPipelineBehavior
import com.opentool.mediatork.mediator.behaviors.TracingPipelineBehavior
import com.opentool.mediatork.mediator.behaviors.UserCreatedNotification
import com.opentool.mediatork.mediator.behaviors.UserRegistrar
import com.opentool.mediatork.mediator.behaviors.ValidationBehavior

val mediator = MediatorFactory.create(
    registrars = listOf(
        UserRegistrar(),
        OrderRegistrar()
    ),
    pipelineBehaviors = listOf(
        LoggingBehavior(),
        ValidationBehavior(),
        RetryPipelineBehavior(),
        TracingPipelineBehavior()
    ),
    preProcessors = listOf(
        AuthPreProcessor()
    ),
    postProcessors = listOf(
        MetricsPostProcessor()
    )
)


suspend fun main() {
    // 🔹 TEST 1: Command
    val orderResult = mediator.send(
        CreateOrderCommand(
            id = "ORD-1",
            amount = 150.0
        )
    )

    println("Final Order Result = $orderResult")

    // 🔹 TEST 2: Query
    val user = mediator.send(
        FetchUserQuery(
            id = "USER-1",
            amount = 0.0
        )
    )

    println("Final User Result = $user")

    // 🔹 TEST 3: Notification (if supported by your mediator)
    mediator.publish(
        UserCreatedNotification(
            id = "USER-1"
        )
    )

    println("Done ✔")
}