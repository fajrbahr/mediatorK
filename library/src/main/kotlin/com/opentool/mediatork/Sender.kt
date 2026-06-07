package com.opentool.mediatork.com.opentool.mediatork

interface Sender {
    suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes
}
