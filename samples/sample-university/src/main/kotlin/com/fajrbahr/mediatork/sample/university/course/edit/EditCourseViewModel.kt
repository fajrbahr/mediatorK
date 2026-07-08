package com.fajrbahr.mediatork.sample.university.course.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditCourseUiState(
    val id: Int = 0,
    val number: Int = 0,
    val title: String = "",
    val credits: String = "",
    val departmentId: Int = 0,
    val errors: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
)

class EditCourseViewModel(
    private val mediator: Mediator,
    private val courseId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(EditCourseUiState())
    val state: StateFlow<EditCourseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val command = mediator.send(EditCourseQuery(courseId))
            if (command != null) {
                _state.value = EditCourseUiState(
                    id = command.id,
                    number = command.number,
                    title = command.title,
                    credits = command.credits.toString(),
                    departmentId = command.departmentId,
                    isLoading = false,
                )
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errors = emptyList()) }
    fun onCreditsChange(value: String) = _state.update { it.copy(credits = value, errors = emptyList()) }
    fun onDepartmentIdChange(value: Int) = _state.update { it.copy(departmentId = value, errors = emptyList()) }

    fun submit() {
        viewModelScope.launch {
            val s = _state.value
            try {
                mediator.send(
                    EditCourseCommand(
                        id = s.id,
                        title = s.title,
                        credits = s.credits.toIntOrNull() ?: -1,
                        departmentId = s.departmentId,
                    )
                )
                _state.update { it.copy(isSaved = true) }
            } catch (e: ValidationException) {
                _state.update { it.copy(errors = e.errors.map { err -> err.toString() }) }
            }
        }
    }
}
