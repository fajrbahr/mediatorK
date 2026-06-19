package com.fajrbahr.mediatork.sample.android.aftersuper.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.AuthorizationPipelineBehavior
import com.fajrbahr.mediatork.pipeline.CachingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.CircuitBreakerPipelineBehavior
import com.fajrbahr.mediatork.pipeline.DeduplicationPipelineBehavior
import com.fajrbahr.mediatork.pipeline.ErrorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.LoggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RateLimitPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RetryPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimeoutPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimingPipelineBehavior
import com.fajrbahr.mediatork.sample.android.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.android.after.model.TodayPrayerTimes
import com.fajrbahr.mediatork.sample.android.aftersuper.domain.AfterSuperRegistrar
import com.fajrbahr.mediatork.sample.android.aftersuper.domain.GetPrayerTimesValidator
import com.fajrbahr.mediatork.sample.android.aftersuper.domain.RequestAuditBehavior
import com.fajrbahr.mediatork.sample.android.aftersuper.domain.TraceIdBehavior
import com.fajrbahr.mediatork.validator.ValidationBehavior
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AfterSuperPrayerTimesViewModel(
    private val mediator: Mediator,
    private val logBuffer: MutableList<String>,
    private val counter: RequestCounterPipelineBehavior,
    private val city: String,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val uiState: StateFlow<AfterSuperUiState> = refreshTrigger
        .flatMapLatest {
            flow {
                emit(AfterSuperUiState.Loading)
                logBuffer.clear()
                val result = runCatching { mediator.send(GetPrayerTimesRequest(city = city)) }
                val count = counter.countFor(GetPrayerTimesRequest::class)
                emit(
                    result.fold(
                        onSuccess = { AfterSuperUiState.Success(it, logBuffer.toList(), count) },
                        onFailure = {
                            AfterSuperUiState.Error(
                                it.message ?: "Failed to load",
                                logBuffer.toList(),
                                count,
                            )
                        },
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AfterSuperUiState.Loading,
        )

    fun retry() {
        viewModelScope.launch { refreshTrigger.emit(Unit) }
    }

    companion object {
        fun factory(city: String) = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                val logs = mutableListOf<String>()
                val counter = RequestCounterPipelineBehavior(order = 20)
                AfterSuperPrayerTimesViewModel(
                    mediator = MediatorFactory.create(
                        registrars = listOf(AfterSuperRegistrar(cache)),
                        pipelineBehaviors = listOf(
                            // -200: outermost — wraps each retry attempt
                            RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
                            // -100: log entry/exit per attempt
                            LoggingPipelineBehavior(
                                logger = { msg -> logs.add(msg); Log.d("MediatorK", msg) },
                                order = -100,
                            ),
                            // -50: fail fast before any expensive processing
                            ValidationBehavior(listOf(GetPrayerTimesValidator()), order = -50),
                            // -10: authorization — only fires for AuthenticatedRequest types
                            AuthorizationPipelineBehavior(order = -10) { context, request ->
                                val traceId = context.getMetaDate<String>("traceId") ?: "no-trace"
                                val line = "[Auth] Authorized ${request::class.simpleName} — $traceId"
                                logs.add(line); Log.d("MediatorK", line)
                            },
                            // -5: rate limit after auth
                            RateLimitPipelineBehavior(maxRequests = 10, windowMs = 10_000, order = -5),
                            // -3: deduplicate concurrent identical requests
                            DeduplicationPipelineBehavior(
                                keyFor = { req -> "${req::class.simpleName}:${(req as? GetPrayerTimesRequest)?.city ?: req.toString()}" },
                                order = -3,
                            ),
                            // -2: cache results — hits skip the handler entirely
                            CachingPipelineBehavior(ttlMs = 30_000, order = -2),
                            // 0: time actual handler execution
                            TimingPipelineBehavior(order = 0) { name, ms ->
                                val line = "⏱ $name took ${ms}ms"
                                logs.add(line); Log.d("MediatorK", line)
                            },
                            // 10: cancel if handler exceeds deadline
                            TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
                            // 15: trip circuit after 5 consecutive failures
                            CircuitBreakerPipelineBehavior(
                                failureThreshold = 5,
                                resetTimeoutMs = 15_000,
                                onStateChange = { state ->
                                    val line = "Circuit: $state"
                                    logs.add(line); Log.d("MediatorK", line)
                                },
                                order = 15,
                            ),
                            // 20: count actual dispatches per request type
                            counter,
                            // Int.MAX_VALUE: innermost — tracks errors closest to the handler
                            ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err ->
                                val line = "❌ ${req::class.simpleName}: ${err.message}"
                                logs.add(line); Log.e("MediatorK", line)
                            },
                            TraceIdBehavior(),
                            RequestAuditBehavior(),
                        ),
                        missingNotificationHandler = SilentMissingNotificationHandler(),
                    ),
                    logBuffer = logs,
                    counter = counter,
                    city = city,
                )
            }
        }
    }
}

sealed interface AfterSuperUiState {
    data object Loading : AfterSuperUiState
    data class Success(val prayerTimes: TodayPrayerTimes, val pipelineLogs: List<String>, val requestCount: Long) :
        AfterSuperUiState

    data class Error(val message: String, val pipelineLogs: List<String>, val requestCount: Long) :
        AfterSuperUiState
}
