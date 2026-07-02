@file:Suppress("TooGenericExceptionCaught")

package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate

/**
 * Simple transaction contract. Implement this in any resource that supports begin/commit/rollback.
 */
interface TransactionProvider {
    suspend fun begin()
    suspend fun commit()
    suspend fun rollback()
}

/**
 * Pipeline behavior that wraps each request in a transaction.
 *
 * Calls [TransactionProvider.begin] before the handler, [TransactionProvider.commit] on success,
 * and [TransactionProvider.rollback] on any exception.
 */
class TransactionPipelineBehavior(
    private val transactionProvider: TransactionProvider,
) : PipelineBehavior {

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        transactionProvider.begin()
        return try {
            val result = next(request)
            transactionProvider.commit()
            result
        } catch (e: Throwable) {
            transactionProvider.rollback()
            throw e
        }
    }
}
