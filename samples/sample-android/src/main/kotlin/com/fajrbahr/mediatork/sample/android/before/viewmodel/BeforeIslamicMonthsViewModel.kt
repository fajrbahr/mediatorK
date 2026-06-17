package com.fajrbahr.mediatork.sample.android.before.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fajrbahr.mediatork.sample.android.before.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.before.data.remote.AladhanRemoteDataSource
import com.fajrbahr.mediatork.sample.android.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.android.before.domain.GetIslamicMonthsUseCase
import com.fajrbahr.mediatork.sample.android.before.model.IslamicMonth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BeforeIslamicMonthsViewModel(
    private val getIslamicMonths: GetIslamicMonthsUseCase,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val uiState: StateFlow<BeforeMonthsUiState> = refreshTrigger
        .flatMapLatest {
            flow {
                emit(BeforeMonthsUiState.Loading)
                emit(
                    runCatching { getIslamicMonths() }
                        .fold(
                            onSuccess = { BeforeMonthsUiState.Success(it) },
                            onFailure = { BeforeMonthsUiState.Error(it.message ?: "Failed to load") },
                        )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BeforeMonthsUiState.Loading,
        )

    fun retry() {
        viewModelScope.launch { refreshTrigger.emit(Unit) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                val repository = AladhanRepository(AladhanRemoteDataSource(), cache)
                BeforeIslamicMonthsViewModel(GetIslamicMonthsUseCase(repository))
            }
        }
    }
}

sealed interface BeforeMonthsUiState {
    data object Loading : BeforeMonthsUiState
    data class Success(val months: List<IslamicMonth>) : BeforeMonthsUiState
    data class Error(val message: String) : BeforeMonthsUiState
}
