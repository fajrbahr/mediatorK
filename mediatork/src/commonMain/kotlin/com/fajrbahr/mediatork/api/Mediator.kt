package com.fajrbahr.mediatork.api

import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow

interface Mediator {
    suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult

    fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T>

    suspend fun <T : Notification> publish(notification: T)

    suspend fun <T : Notification> publish(notification: T, strategy: NotificationPublishStrategy)
}
