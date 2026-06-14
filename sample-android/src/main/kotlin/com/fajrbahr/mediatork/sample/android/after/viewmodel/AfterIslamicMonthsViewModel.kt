package com.fajrbahr.mediatork.sample.android.after.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.android.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.android.after.domain.IslamicMonthsRegistrar
import com.fajrbahr.mediatork.sample.android.after.model.IslamicMonth
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
                    MediatorFactory.create(registrars = listOf(IslamicMonthsRegistrar(cache)))
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
