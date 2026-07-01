package com.fajrbahr.mediatork.sample.university.student.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.student.domain.CreateStudentCommand
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateStudentUiState(
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
    val errors: List<String> = emptyList(),
    val isSaved: Boolean = false,
)

class CreateStudentViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow(CreateStudentUiState())
    val state: StateFlow<CreateStudentUiState> = _state.asStateFlow()

    fun onLastNameChange(value: String) = _state.update { it.copy(lastName = value, errors = emptyList()) }
    fun onFirstMidNameChange(value: String) = _state.update { it.copy(firstMidName = value, errors = emptyList()) }
    fun onEnrollmentDateChange(value: String) = _state.update { it.copy(enrollmentDate = value, errors = emptyList()) }

    fun submit() {
        viewModelScope.launch {
            val s = _state.value
            try {
                mediator.send(
                    CreateStudentCommand(
                        lastName = s.lastName,
                        firstMidName = s.firstMidName,
                        enrollmentDate = s.enrollmentDate,
                    )
                )
                _state.update { it.copy(isSaved = true) }
            } catch (e: ValidationException) {
                _state.update { it.copy(errors = e.errors.map { err -> err.toString() }) }
            }
        }
    }
}
