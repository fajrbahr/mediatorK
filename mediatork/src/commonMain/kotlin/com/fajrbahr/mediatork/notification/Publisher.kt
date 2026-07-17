package com.fajrbahr.mediatork.notification

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Notification

suspend fun Mediator.publishDynamic(notification: Any) {
    require(notification is Notification) { "publishDynamic: $notification does not implement Notification" }
    publish(notification)
}
