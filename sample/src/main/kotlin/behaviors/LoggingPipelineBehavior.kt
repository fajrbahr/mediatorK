package com.opentool.mediatork.mediator.behaviors

import com.opentool.mediatork.com.opentool.mediatork.HandlerRegistry
import com.opentool.mediatork.com.opentool.mediatork.MediatorRegistrar
import com.opentool.mediatork.com.opentool.mediatork.Notification
import com.opentool.mediatork.com.opentool.mediatork.NotificationHandler
import com.opentool.mediatork.com.opentool.mediatork.PipelineBehavior
import com.opentool.mediatork.com.opentool.mediatork.Request
import com.opentool.mediatork.com.opentool.mediatork.RequestContext
import com.opentool.mediatork.com.opentool.mediatork.RequestHandler
import com.opentool.mediatork.com.opentool.mediatork.RequestHandlerDelegate
import com.opentool.mediatork.com.opentool.mediatork.RequestPostProcessor
import com.opentool.mediatork.com.opentool.mediatork.RequestPreProcessor
import kotlin.system.measureTimeMillis

class LoggingBehavior : PipelineBehavior {
    override val order: Int = 0

    override suspend fun <TReq : Request<TRes>, TRes> behave(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq
    ): TRes {
        println("Mediator.Log --> Request: ${request::class.simpleName}")
        val result = next(request)
        println("Mediator.Log <-- Response: $result")
        return result
    }
    }

class ValidationBehavior : PipelineBehavior {
    override val order: Int = 0

    override suspend fun <TReq : Request<TRes>, TRes> behave(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq
    ): TRes {
        val result = next(request)
        return result
    }
    }

class UserRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(FetchUserQuery::class, FetchUserHandler())
        registry.registerNotification(UserCreatedNotification ::class, UserCreatedNotificationHandler())

    }
}
class OrderRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(CreateOrderCommand::class, CreateOrderHandler())
    }
}

data class CreateOrderCommand(
    val id: String,
    val amount: Double
) : Request<OrderResult>

data class UserCreatedNotification(
    val id: String,
) : Notification

data class OrderResult(
    val success: Boolean
)
data class User(
    val name: String
)

class RetryPipelineBehavior(
    private val maxRetries: Int = 2
) : PipelineBehavior {

    override val order: Int = 1

    override suspend fun <TReq : Request<TRes>, TRes> behave(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq
    ): TRes {

        var lastError: Throwable? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return next(request)
            } catch (t: Throwable) {
                lastError = t
                println("Retry attempt ${attempt + 1} failed for ${request::class.simpleName}")
            }
        }

        throw lastError ?: RuntimeException("Unknown error")
    }
}

class TracingPipelineBehavior : PipelineBehavior {

    override val order: Int = 10

    override suspend fun <TReq : Request<TRes>, TRes> behave(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq
    ): TRes {

        val requestName = request::class.simpleName ?: "UnknownRequest"

        println(" TRACE ➜ Start: $requestName")

        val result: TRes
        val duration = measureTimeMillis {
            result = next(request)
        }

        println(" TRACE ⬅ End: $requestName in ${duration}ms")

        return result
    }
}

class CreateOrderHandler : RequestHandler<CreateOrderCommand,OrderResult> {

    override suspend fun handle(
        requestContext: RequestContext,
        request: CreateOrderCommand
    ): OrderResult {
        println("Creating order: ${request.id}")
        return OrderResult(success = true)
    }
}

class UserCreatedNotificationHandler : NotificationHandler<UserCreatedNotification> {

    override suspend fun handle(notification: UserCreatedNotification) {
        println("User created event received: ${notification.id}")
    }
}

data class FetchUserQuery(
    val id: String,
    val amount: Double
) : Request<User>

class FetchUserHandler : RequestHandler<FetchUserQuery,User> {

    override suspend fun handle(
        requestContext: RequestContext,
        request: FetchUserQuery
    ): User {
        println("User : ${request.id}")
        return User("ali")
    }
}

class AuthPreProcessor : RequestPreProcessor{
    override suspend fun process(
        requestContext: RequestContext,
        request: Request<*>
    ) {
        requestContext.put("authToken", "mock-token")
    }

}

class MetricsPostProcessor : RequestPostProcessor {
    override suspend fun process(
        requestContext: RequestContext,
        request: Request<*>,
        response: Any?
    ) {
        println("📊 Metrics for ${request::class.simpleName}")
    }

}
