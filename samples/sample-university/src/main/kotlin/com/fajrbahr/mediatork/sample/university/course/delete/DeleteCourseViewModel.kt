package com.fajrbahr.mediatork.sample.university.course.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteCourseUiState(
    val model: DeleteCourseCommand? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class DeleteCourseViewModel(
    private val mediator: Mediator,
    private val courseId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteCourseUiState())
    val state: StateFlow<DeleteCourseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val model = mediator.send(DeleteCourseQuery(courseId))
            _state.value = DeleteCourseUiState(model = model, isLoading = false)
        }
    }

    fun delete() {
        val model = _state.value.model ?: return
        viewModelScope.launch {
            mediator.send(model)
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
