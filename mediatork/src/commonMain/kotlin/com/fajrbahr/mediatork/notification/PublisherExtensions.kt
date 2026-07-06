package com.fajrbahr.mediatork.notification

import com.fajrbahr.mediatork.api.Notification

/** Lambda syntax: `publish { OrderCreatedNotification(...) }` */
suspend inline fun <reified T : Notification> Publisher.publish(block: () -> T) {
    publish(block())
}
