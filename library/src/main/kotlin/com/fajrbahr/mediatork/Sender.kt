package com.fajrbahr.mediatork

interface Sender {
    suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes
}
