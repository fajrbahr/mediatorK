package com.fajrbahr.mediatork.sample.android.after.times

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AfterPrayerTimesViewModel(
    private val mediator: Mediator,
    private val city: String,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val uiState: StateFlow<AfterUiState> = refreshTrigger
        .flatMapLatest {
            flow {
                emit(AfterUiState.Loading)
                emit(
                    runCatching { mediator.send(GetPrayerTimesRequest(city = city)) }
                        .fold(
                            onSuccess = { AfterUiState.Success(it) },
                            onFailure = { AfterUiState.Error(it.message ?: "Failed to load prayer times") },
                        )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AfterUiState.Loading,
        )

    fun retry() {
        viewModelScope.launch { refreshTrigger.emit(Unit) }
    }

    companion object {
        fun factory(city: String) = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                AfterPrayerTimesViewModel(
                    MediatorFactory.create(registrars = listOf(PrayerTimesRegistrar(cache))),
                    city,
                )
            }
        }
    }
}

sealed interface AfterUiState {
    data object Loading : AfterUiState
    data class Success(val prayerTimes: TodayPrayerTimes) : AfterUiState
    data class Error(val message: String) : AfterUiState
}
