package com.fajrbahr.mediatork.sample.android.after.islamicMonths

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AfterIslamicMonthsViewModel(private val mediator: Mediator) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val uiState: StateFlow<AfterMonthsUiState> = refreshTrigger
        .flatMapLatest {
            flow {
                emit(AfterMonthsUiState.Loading)
                emit(
                    runCatching { mediator.send(GetIslamicMonthsRequest()) }
                        .fold(
                            onSuccess = { AfterMonthsUiState.Success(it) },
                            onFailure = { AfterMonthsUiState.Error(it.message ?: "Failed to load") },
                        )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AfterMonthsUiState.Loading,
        )

    fun retry() {
        viewModelScope.launch { refreshTrigger.emit(Unit) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                AfterIslamicMonthsViewModel(
                    mediatorK { islamicMonthsModule(cache) }
                )
            }
        }
    }
}

sealed interface AfterMonthsUiState {
    data object Loading : AfterMonthsUiState
    data class Success(val months: List<IslamicMonth>) : AfterMonthsUiState
    data class Error(val message: String) : AfterMonthsUiState
}
