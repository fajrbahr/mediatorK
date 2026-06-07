package com.opentool.mediatork.com.opentool.mediatork.functional

/**
 * Iterates over every registered request handler and confirms it resolves without throwing.
 * Call this once at app startup (after Koin loads all modules) to surface missing-handler bugs
 * early-as a warning log rather than a runtime crash at first use.
 *
 * @param onMissingHandler called for each request type that has no handler registered.
 *   Defaults to a no-op; callers typically provide a logger.warning invocation.
 */
fun HandlerRegistry.verifyHandlers(
    onMissingHandler: (typeName: String) -> Unit = {},
) {
    requestHandlers.keys.forEach { requestType ->
        if (!hasHandler(requestType)) {
            onMissingHandler(requestType.simpleName ?: "UnknownRequest")
        }
    }
}
