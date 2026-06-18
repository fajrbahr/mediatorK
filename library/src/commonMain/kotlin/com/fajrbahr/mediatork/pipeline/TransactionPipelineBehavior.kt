package com.fajrbahr.mediatork.pipeline

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext

/**
 * Abstraction over a transactional unit of work.
 *
 * Implement this to adapt any persistence layer — Room, Exposed, SQLDelight, etc.
 * The [withTransaction] block must commit on normal return and roll back on exception.
 *
 * ```kotlin
 * // Room
 * val provider = object : TransactionProvider {
 *     override suspend fun <T> withTransaction(block: suspend () -> T): T =
 *         db.withTransaction { block() }
 * }
 *
 * // Exposed
 * val provider = object : TransactionProvider {
 *     override suspend fun <T> withTransaction(block: suspend () -> T): T =
 *         newSuspendedTransaction { block() }
 * }
 * ```
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
 * [com.fajrbahr.mediatork.Request.Unit] marker is in use, a common pattern is to limit
 * transactions to those requests:
 *
 * ```kotlin
 * TransactionPipelineBehavior(
 *     transactionProvider = TransactionProvider { block -> db.withTransaction { block() } },
 *     appliesTo = { it is Request.Unit },
 * )
 * ```
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
