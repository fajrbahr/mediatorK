package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestHandlerDelegate

/**
 * Abstraction over a transactional unit of work.
 *
 * Implement this to adapt any persistence layer — Room, Exposed, SQLDelight, etc.
 * The [withTransaction] block must commit on normal return and roll back on exception.
 *
 */
interface TransactionProvider {
    suspend fun <T> withTransaction(block: suspend () -> T): T
}

/**
 * A [PipelineBehavior] that wraps each matching request in a transaction.
 *
 * The transaction commits when the handler returns normally, and rolls back if it throws.
 * The original exception is re-thrown after rollback so callers can handle or log it.
 *
 * Use the [appliesTo] predicate to restrict transactions to write operations. When the
 * [Request.Unit] marker is in use, a common pattern is to limit
 * transactions to those requests:
 *
 *
 * @param transactionProvider the unit-of-work implementation for the underlying store.
 * @param appliesTo predicate deciding which requests run inside a transaction; defaults to all.
 * @param order position in the behavior chain. Defaults to `0`.
 */
class TransactionPipelineBehavior(
    private val transactionProvider: TransactionProvider,
    private val appliesTo: (Request<*>) -> Boolean = { true },
    override val order: Int = 0,
) : PipelineBehavior {

    override fun appliesTo(request: Request<*>): Boolean = appliesTo.invoke(request)

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult = transactionProvider.withTransaction { next(request) }
}
