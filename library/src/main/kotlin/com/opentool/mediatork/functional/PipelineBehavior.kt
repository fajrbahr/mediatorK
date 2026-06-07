package com.opentool.mediatork.com.opentool.mediatork.functional

typealias RequestHandlerDelegate<TReq, TRes> = suspend (TReq) -> TRes

interface PipelineBehavior {

    companion object {
        const val HIGHEST_PRECEDENCE = Int.MIN_VALUE
        const val LOWEST_PRECEDENCE = Int.MAX_VALUE
    }

    val order: Int get() = 0

    fun appliesTo(request: Request<*>): Boolean = true

    suspend fun <TReq : Request<TRes>, TRes> behave(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq,
    ): TRes
}
