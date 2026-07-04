package com.fajrbahr.mediatork.api

/**
 * Common interface for all pipeline behaviors (request and stream).
 * Allows mixing [PipelineBehavior] and [StreamPipelineBehavior] in a single call.
 */
sealed interface Behavior
