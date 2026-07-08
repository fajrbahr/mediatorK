package com.fajrbahr.mediatork.sample.university.course.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateCourseUiState(
    val number: String = "",
    val title: String = "",
    val credits: String = "",
    val departmentId: Int = 0,
    val errors: List<String> = emptyList(),
    val isSaved: Boolean = false,
)

class CreateCourseViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow(CreateCourseUiState())
    val state: StateFlow<CreateCourseUiState> = _state.asStateFlow()

    fun onNumberChange(value: String) = _state.update { it.copy(number = value, errors = emptyList()) }
    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errors = emptyList()) }
    fun onCreditsChange(value: String) = _state.update { it.copy(credits = value, errors = emptyList()) }
    fun onDepartmentIdChange(value: Int) = _state.update { it.copy(departmentId = value, errors = emptyList()) }

    fun submit() {
        viewModelScope.launch {
            val s = _state.value
            try {
                mediator.send(
                    CreateCourseCommand(
                        number = s.number.toIntOrNull() ?: 0,
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
