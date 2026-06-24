package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.pipeline.buildin.TransactionPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.TransactionProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TransactionPipelineBehaviorTest {

    private class RecordingTransactionProvider : TransactionProvider {
        val log = mutableListOf<String>()
        override suspend fun begin() { log += "begin" }
        override suspend fun commit() { log += "commit" }
        override suspend fun rollback() { log += "rollback" }
    }

    @Test
    fun `begin and commit called on successful handler`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(TransactionPipelineBehavior(provider))) {
            register(PingHandler())
        }
        m.send(PingQuery("x"))
        assertEquals(listOf("begin", "commit"), provider.log)
    }

    @Test
    fun `begin and rollback called when handler throws`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(TransactionPipelineBehavior(provider))) {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
                    throw RuntimeException("handler failed")
            })
        }
        assertFailsWith<RuntimeException> { m.send(PingQuery("x")) }
        assertEquals(listOf("begin", "rollback"), provider.log)
    }

    @Test
    fun `exception is rethrown after rollback`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(TransactionPipelineBehavior(provider))) {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
                    throw IllegalStateException("domain error")
            })
        }
        val ex = assertFailsWith<IllegalStateException> { m.send(PingQuery("x")) }
        assertEquals("domain error", ex.message)
    }

    @Test
    fun `handler result returned on success`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(TransactionPipelineBehavior(provider))) {
            register(AddHandler())
        }
        assertEquals(7, m.send(AddCommand(3, 4)))
    }

    @Test
    fun `commit not called when handler throws`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(TransactionPipelineBehavior(provider))) {
            register(object : RequestHandler<PingQuery, String> {
                override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: PingQuery): String =
                    throw RuntimeException("boom")
            })
        }
        runCatching { m.send(PingQuery("x")) }
        assertFalse("commit" in provider.log)
    }

    @Test
    fun `transaction wraps each request independently`() = runTest {
        val provider = RecordingTransactionProvider()
        val m = mediator(pipelineBehaviors = listOf(TransactionPipelineBehavior(provider))) {
            register(PingHandler())
        }
        m.send(PingQuery("a"))
        m.send(PingQuery("b"))
        assertEquals(listOf("begin", "commit", "begin", "commit"), provider.log)
    }
}
