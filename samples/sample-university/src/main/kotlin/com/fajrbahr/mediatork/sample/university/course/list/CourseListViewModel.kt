package com.fajrbahr.mediatork.sample.university.course.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.course.detail.DeleteCourseCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CourseListUiState {
    data object Loading : CourseListUiState
    data class Success(val courses: List<CourseListModel>) : CourseListUiState
    data class Error(val message: String) : CourseListUiState
}

class CourseListViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow<CourseListUiState>(CourseListUiState.Loading)
    val state: StateFlow<CourseListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = CourseListUiState.Loading
            runCatching { mediator.send(GetCoursesQuery) }
                .fold(
                    onSuccess = { _state.value = CourseListUiState.Success(it.courses) },
                    onFailure = { _state.value = CourseListUiState.Error(it.message ?: "Unknown error") },
                )
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            mediator.send(DeleteCourseCommand(id))
            load()
        }
    }
}
