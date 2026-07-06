@file:Suppress("TooGenericExceptionThrown")

package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.TransactionProvider
import com.fajrbahr.mediatork.pipeline.buildin.transactionPipelineBehavior
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class TransactionPipelineBehaviorTest {

    private class RecordingTransactionProvider : TransactionProvider {
        val log = mutableListOf<String>()
        override suspend fun begin() {
            log += "begin"
        }

        override suspend fun commit() {
            log += "commit"
        }

        override suspend fun rollback() {
            log += "rollback"
        }
    }

    @Test
    fun `begin and commit called on successful handler`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(PingHandler())
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("begin", "commit"), provider.log)
    }

    @Test
    fun `begin and rollback called when handler throws`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(RequestHandler<PingQuery, String> { mediator, requestContext, request -> throw RuntimeException("handler failed") })
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals(listOf("begin", "rollback"), provider.log)
    }

    @Test
    fun `exception is rethrown after rollback`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(RequestHandler<PingQuery, String> { mediator, requestContext, request -> error("domain error") })
        }
        val ex = assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
        assertEquals("domain error", ex.message)
    }

    @Test
    fun `handler result returned on success`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(AddHandler())
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
    }

    @Test
    fun `commit not called when handler throws`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(RequestHandler<PingQuery, String> { mediator, requestContext, request -> throw RuntimeException("boom") })
        }
        runCatching { m.send(PingQuery("x")) }
        assertFalse("commit" in provider.log)
    }

    @Test
    fun `transaction wraps each request independently`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(PingHandler())
        }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(listOf("begin", "commit", "begin", "commit"), provider.log)
    }

    @Test
    fun `rollback is not called on success`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(PingHandler())
        }
        m.send(PingQuery("x"))
        assertFalse("rollback" in provider.log)
    }

    @Test
    fun `begin is called before handler and commit after`() = runTest {
        val combined = mutableListOf<String>()
        val provider = object : TransactionProvider {
            override suspend fun begin() {
                combined += "begin"
            }

            override suspend fun commit() {
                combined += "commit"
            }

            override suspend fun rollback() {
                combined += "rollback"
            }
        }
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(RequestHandler<PingQuery, String> { mediator, requestContext, request ->
                combined += "handler"
                "ok"
            })
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("begin", "handler", "commit"), combined)
    }

    @Test
    fun `Unit handler commits transaction correctly`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(transactionPipelineBehavior(provider))) {
            handler(NoResultHandler())
        }
        m.send(NoResultCommand("test-id"))
        assertEquals(listOf("begin", "commit"), provider.log)
    }
}
