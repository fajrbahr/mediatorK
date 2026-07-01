package com.fajrbahr.mediatork.sample.university.course.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.course.domain.DeleteCourseCommand
import com.fajrbahr.mediatork.sample.university.course.domain.GetCourseQuery
import com.fajrbahr.mediatork.sample.university.course.model.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val course: Course? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class CourseDetailViewModel(
    private val mediator: Mediator,
    private val courseId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(CourseDetailUiState())
    val state: StateFlow<CourseDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val course = mediator.send(GetCourseQuery(courseId))
            _state.value = CourseDetailUiState(course = course, isLoading = false)
        }
    }

    fun delete() {
        viewModelScope.launch {
            mediator.send(DeleteCourseCommand(courseId))
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
