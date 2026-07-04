@file:Suppress("TooGenericExceptionCaught")

package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.feature.behavior

/**
 * Simple transaction contract. Implement this in any resource that supports begin/commit/rollback.
 */
interface TransactionProvider {
    suspend fun begin()
    suspend fun commit()
    suspend fun rollback()
}

/**
 * Behavior provider that wraps each request in a transaction.
 *
 * Calls [TransactionProvider.begin] before the handler, [TransactionProvider.commit] on success,
 * and [TransactionProvider.rollback] on any exception.
 */
fun transactionPipelineBehavior(
    transactionProvider: TransactionProvider,
    order: Int = 0,
): PipelineBehavior = behavior(order = order) { _, next, request ->
    transactionProvider.begin()
    try {
        val result = next(request)
        transactionProvider.commit()
        result
    } catch (e: Throwable) {
        transactionProvider.rollback()
        throw e
    }
}
