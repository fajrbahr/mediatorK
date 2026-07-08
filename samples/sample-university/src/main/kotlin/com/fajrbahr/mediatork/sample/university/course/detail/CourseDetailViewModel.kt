package com.fajrbahr.mediatork.sample.university.course.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val model: CourseDetailModel? = null,
    val isLoading: Boolean = true,
)

class CourseDetailViewModel(
    private val mediator: Mediator,
    private val courseId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(CourseDetailUiState())
    val state: StateFlow<CourseDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val model = mediator.send(GetCourseQuery(courseId))
            _state.value = CourseDetailUiState(model = model, isLoading = false)
        }
    }

}
