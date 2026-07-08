package com.fajrbahr.mediatork.sample.university.instructor.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.instructor.detail.DeleteInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InstructorListUiState {
    data object Loading : InstructorListUiState
    data class Success(val instructors: List<Instructor>) : InstructorListUiState
    data class Error(val message: String) : InstructorListUiState
}

class InstructorListViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow<InstructorListUiState>(InstructorListUiState.Loading)
    val state: StateFlow<InstructorListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = InstructorListUiState.Loading
            runCatching { mediator.send(GetInstructorsQuery) }
                .fold(
                    onSuccess = { _state.value = InstructorListUiState.Success(it) },
                    onFailure = { _state.value = InstructorListUiState.Error(it.message ?: "Unknown error") },
                )
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            mediator.send(DeleteInstructorCommand(id))
            load()
        }
    }
}
