package com.opentool.mediatork.com.opentool.mediatork.functional

typealias NotificationHandler<T> = suspend (T) -> Unit
