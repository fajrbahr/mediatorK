package sample

import com.fajrbahr.mediatork.*
import kotlinx.coroutines.test.runTest
import sample.query.FetchUserQuery
import sample.query.User
import sample.notification.OrderCreatedNotification
import kotlin.test.*

/**
 * Shows how to test MediatorK handlers without any mocking library.
 *
 * The pattern:
 * - Implement the interface with the minimum code needed for the test case.
 * - Wire everything through MediatorFactory — same path as production.
 * - No mock frameworks, no reflection tricks, no special test utilities.
 */
class SampleHandlerTest {

    // ── Stub handler — returns a fixed value ──────────────────────────────────

    @Test
    fun `handler returns expected result`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object : RequestHandler<FetchUserQuery, User> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: FetchUserQuery,
                        ) = User(name = "Huzaifa", email = "huzaifa@beno.com")
                    }
                }
            })
        )

        val user = mediator.send(FetchUserQuery(id = "1", amount = 0.0))

        assertEquals("Huzaifa", user.name)
        assertEquals("huzaifa@beno.com", user.email)
    }

    // ── Empty handler — just records it was called ────────────────────────────

    @Test
    fun `command handler is invoked with correct request`() = runTest {
        var receivedId: String? = null

        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object : RequestHandler<FetchUserQuery, User> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: FetchUserQuery,
                        ): User {
                            receivedId = request.id
                            return User("", "")
                        }
                    }
                }
            })
        )

        mediator.send(FetchUserQuery(id = "user-42", amount = 0.0))

        assertEquals("user-42", receivedId)
    }

    // ── Notification — verify all subscribers are called ─────────────────────

    @Test
    fun `all notification handlers receive the published event`() = runTest {
        val received = mutableListOf<String>()

        val makeHandler = { tag: String ->
            object : NotificationHandler<OrderCreatedNotification> {
                override suspend fun handle(notification: OrderCreatedNotification) {
                    received += "$tag:${notification.orderId}"
                }
            }
        }

        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry registerNotification makeHandler("email")
                    registry registerNotification makeHandler("sms")
                    registry registerNotification makeHandler("analytics")
                }
            })
        )

        mediator.publish(
            OrderCreatedNotification(
                orderId = "ORD-1",
                customerEmail = "user@beno.com",
                customerPhone = "+971500000000",
                totalAmount = 500.0,
            )
        )

        assertEquals(3, received.size)
        assertTrue("email:ORD-1" in received)
        assertTrue("sms:ORD-1" in received)
        assertTrue("analytics:ORD-1" in received)
    }

    // ── MissingHandlerException — verify error path ───────────────────────────

    @Test
    fun `send throws when no handler registered`() = runTest {
        val mediator = MediatorFactory.create(registrars = emptyList())

        assertFailsWith<MissingHandlerException> {
            mediator.send(FetchUserQuery(id = "x", amount = 0.0))
        }
    }

    // ── Pipeline behavior — verify cross-cutting logic runs ───────────────────

    @Test
    fun `pipeline behavior executes around handler`() = runTest {
        val log = mutableListOf<String>()

        val loggingBehavior = object : PipelineBehavior {
            override suspend fun <TReq : Request<TRes>, TRes> process(
                requestContext: RequestContext,
                next: RequestHandlerDelegate<TReq, TRes>,
                request: TReq,
            ): TRes {
                log += "before"
                return next(request).also { log += "after" }
            }
        }

        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register object : RequestHandler<FetchUserQuery, User> {
                        override suspend fun handle(
                            mediator: Mediator,
                            requestContext: RequestContext,
                            request: FetchUserQuery,
                        ): User {
                            log += "handler"
                            return User("", "")
                        }
                    }
                }
            }),
            pipelineBehaviors = listOf(loggingBehavior),
        )

        mediator.send(FetchUserQuery(id = "1", amount = 0.0))

        assertEquals(listOf("before", "handler", "after"), log)
    }
}
