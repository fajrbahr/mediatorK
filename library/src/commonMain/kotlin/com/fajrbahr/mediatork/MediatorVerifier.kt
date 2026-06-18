package com.fajrbahr.mediatork

/**
 * Iterates over every registered request type and confirms a handler exists for it.
 *
 * Call this once at application startup — after all [MediatorRegistrar]s have run —
 * to surface missing-handler misconfigurations as early warnings rather than
 * runtime crashes at first use. [MediatorFactory.create] invokes this automatically.
 *
 * @receiver the registry to inspect.
 * @param onMissingHandler invoked for each request type that has no registered handler.
 *   Defaults to a no-op; callers typically supply a logger or `println` call.
 */
fun HandlerRegistry.verifyHandlers(
    onMissingHandler: (typeName: String) -> Unit = {},
) {
    requestHandlers.keys.forEach { requestType ->
        if (!hasHandler(requestType)) {
            onMissingHandler(requestType.simpleName ?: "UnknownRequest")
        }
    }
    streamHandlers.keys.forEach { requestType ->
        if (!hasStreamHandler(requestType)) {
            onMissingHandler(requestType.simpleName ?: "UnknownStreamRequest")
        }
    }
}
