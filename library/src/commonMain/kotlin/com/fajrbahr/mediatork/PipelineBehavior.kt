package com.fajrbahr.mediatork

typealias RequestHandlerDelegate<TReq, TRes> = suspend (TReq) -> TRes

interface PipelineBehavior {

    val order: Int get() = 0

    val isEnabled: Boolean get() = true

    fun appliesTo(request: Request<*>): Boolean = true

    suspend fun <TReq : Request<TRes>, TRes> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq,
    ): TRes
}
