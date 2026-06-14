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
import com.fajrbahr.mediatork.sample.android.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.android.after.domain.IslamicMonthsRegistrar
import android.util.Log
import com.fajrbahr.mediatork.sample.android.after.model.IslamicMonth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AfterSuperIslamicMonthsViewModel(
    private val mediator: Mediator,
    private val logBuffer: MutableList<String>,
    private val counter: RequestCounterPipelineBehavior,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val uiState: StateFlow<AfterSuperMonthsUiState> = refreshTrigger
        .flatMapLatest {
            flow {
                emit(AfterSuperMonthsUiState.Loading)
                logBuffer.clear()
                val result = runCatching { mediator.send(GetIslamicMonthsRequest()) }
                val count = counter.countFor(GetIslamicMonthsRequest::class)
                emit(
                    result.fold(
                        onSuccess = { AfterSuperMonthsUiState.Success(it, logBuffer.toList(), count) },
                        onFailure = { AfterSuperMonthsUiState.Error(it.message ?: "Failed to load", logBuffer.toList(), count) },
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AfterSuperMonthsUiState.Loading,
        )

    fun retry() { viewModelScope.launch { refreshTrigger.emit(Unit) } }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                val logs = mutableListOf<String>()
                val counter = RequestCounterPipelineBehavior(order = 20)
                AfterSuperIslamicMonthsViewModel(
                    mediator = MediatorFactory.create(
                        registrars = listOf(IslamicMonthsRegistrar(cache)),
                        pipelineBehaviors = listOf(
                            RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
                            LoggingPipelineBehavior(logger = { msg -> logs.add(msg); Log.d("MediatorK", msg) }, order = -100),
                            TimingPipelineBehavior(order = 0) { name, ms ->
                                val line = "⏱ $name took ${ms}ms"
                                logs.add(line)
                                Log.d("MediatorK", line)
                            },
                            TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
                            counter,
                            ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err ->
                                val line = "❌ ${req::class.simpleName}: ${err.message}"
                                logs.add(line)
                                Log.e("MediatorK", line)
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

sealed interface AfterSuperMonthsUiState {
    data object Loading : AfterSuperMonthsUiState
    data class Success(val months: List<IslamicMonth>, val pipelineLogs: List<String>, val requestCount: Long) : AfterSuperMonthsUiState
    data class Error(val message: String, val pipelineLogs: List<String>, val requestCount: Long) : AfterSuperMonthsUiState
}
