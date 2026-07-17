package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*

// ── Request / Notification types ─────────────────────────────────────────────

data class PingQuery(val value: String) : Request<String>
data class AddCommand(val a: Int, val b: Int) : Request<Int>
data class NoResultCommand(val id: String) : Request.Unit
data class EchoQuery(val text: String) : Request<String>
data class PingNotification(val message: String) : Notification
data class AlertNotification(val level: Int) : Notification
