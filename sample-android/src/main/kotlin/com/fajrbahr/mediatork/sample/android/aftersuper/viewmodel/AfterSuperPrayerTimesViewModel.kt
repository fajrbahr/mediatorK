package com.fajrbahr.mediatork.sample.android.aftersuper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.pipeline.ErrorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.LoggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RetryPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimeoutPipelineBehavior
import com.fajrbahr.mediatork.sample.android.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.android.after.domain.PrayerTimesRegistrar
import com.fajrbahr.mediatork.sample.android.after.model.TodayPrayerTimes
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
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val uiState: StateFlow<AfterSuperUiState> = refreshTrigger
        .flatMapLatest {
            flow {
                emit(AfterSuperUiState.Loading)
                logBuffer.clear()
                val result = runCatching { mediator.send(GetPrayerTimesRequest()) }
                val count = counter.countFor(GetPrayerTimesRequest::class)
                emit(
                    result.fold(
                        onSuccess = { AfterSuperUiState.Success(it, logBuffer.toList(), count) },
                        onFailure = { AfterSuperUiState.Error(it.message ?: "Failed to load", logBuffer.toList(), count) },
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AfterSuperUiState.Loading,
        )

    fun retry() { viewModelScope.launch { refreshTrigger.emit(Unit) } }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                val logs = mutableListOf<String>()
                val counter = RequestCounterPipelineBehavior(order = 20)
                AfterSuperPrayerTimesViewModel(
                    mediator = MediatorFactory.create(
                        registrars = listOf(PrayerTimesRegistrar(cache)),
                        pipelineBehaviors = listOf(
                            RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
                            LoggingPipelineBehavior(logger = { msg -> logs.add(msg) }, order = -100),
                            TimingPipelineBehavior(order = 0) { name, ms -> logs.add("⏱ $name took ${ms}ms") },
                            TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
                            counter,
                            ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err ->
                                logs.add("❌ ${req::class.simpleName}: ${err.message}")
                            },
                        ),
                    ),
                    logBuffer = logs,
                    counter = counter,
                )
            }
        }
    }
}

sealed interface AfterSuperUiState {
    data object Loading : AfterSuperUiState
    data class Success(val prayerTimes: TodayPrayerTimes, val pipelineLogs: List<String>, val requestCount: Long) : AfterSuperUiState
    data class Error(val message: String, val pipelineLogs: List<String>, val requestCount: Long) : AfterSuperUiState
}
