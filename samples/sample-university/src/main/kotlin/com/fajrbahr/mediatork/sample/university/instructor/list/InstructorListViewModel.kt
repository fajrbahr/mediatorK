package com.fajrbahr.mediatork.sample.university.instructor.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InstructorListUiState {
    data object Loading : InstructorListUiState
    data class Success(val model: InstructorIndexModel) : InstructorListUiState
    data class Error(val message: String) : InstructorListUiState
}

class InstructorListViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow<InstructorListUiState>(InstructorListUiState.Loading)
    val state: StateFlow<InstructorListUiState> = _state.asStateFlow()

    private var selectedInstructorId: Int? = null
    private var selectedCourseId: Int? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = InstructorListUiState.Loading
            runCatching {
                mediator.send(
                    GetInstructorsQuery(
                        selectedInstructorId = selectedInstructorId,
                        selectedCourseId = selectedCourseId,
                    )
                )
            }.fold(
                onSuccess = { _state.value = InstructorListUiState.Success(it) },
                onFailure = { _state.value = InstructorListUiState.Error(it.message ?: "Unknown error") },
            )
        }
    }

    fun selectInstructor(id: Int) {
        selectedInstructorId = if (selectedInstructorId == id) null else id
        selectedCourseId = null
        load()
    }

    fun selectCourse(id: Int) {
        selectedCourseId = if (selectedCourseId == id) null else id
        load()
    }
}
