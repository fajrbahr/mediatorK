package com.fajrbahr.mediatork.sample.android.before.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fajrbahr.mediatork.sample.android.before.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.before.data.remote.AladhanRemoteDataSource
import com.fajrbahr.mediatork.sample.android.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.android.before.domain.GetPrayerTimesUseCase
import com.fajrbahr.mediatork.sample.android.before.model.TodayPrayerTimes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BeforePrayerTimesViewModel(
    private val getPrayerTimes: GetPrayerTimesUseCase,
    private val city: String,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val uiState: StateFlow<BeforeUiState> = refreshTrigger
        .flatMapLatest {
            flow {
                emit(BeforeUiState.Loading)
                emit(
                    runCatching { getPrayerTimes(city) }
                        .fold(
                            onSuccess = { BeforeUiState.Success(it) },
                            onFailure = { BeforeUiState.Error(it.message ?: "Failed to load") },
                        )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BeforeUiState.Loading,
        )

    fun retry() {
        viewModelScope.launch { refreshTrigger.emit(Unit) }
    }

    companion object {
        fun factory(city: String) = viewModelFactory {
            initializer {
                val cache = AladhanCacheDataSource()
                val repository = AladhanRepository(AladhanRemoteDataSource(), cache)
                BeforePrayerTimesViewModel(GetPrayerTimesUseCase(repository), city)
            }
        }
    }
}

sealed interface BeforeUiState {
    data object Loading : BeforeUiState
    data class Success(val prayerTimes: TodayPrayerTimes) : BeforeUiState
    data class Error(val message: String) : BeforeUiState
}
