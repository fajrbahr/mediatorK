package com.fajrbahr.mediatork.sample.university.student.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditStudentUiState(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
    val errors: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
)

class EditStudentViewModel(
    private val mediator: Mediator,
    private val studentId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(EditStudentUiState())
    val state: StateFlow<EditStudentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val command = mediator.send(EditStudentQuery(studentId))
            if (command != null) {
                _state.value = EditStudentUiState(
                    id = command.id,
                    lastName = command.lastName,
                    firstMidName = command.firstMidName,
                    enrollmentDate = command.enrollmentDate,
                    isLoading = false,
                )
            }
        }
    }

    fun onLastNameChange(value: String) = _state.update { it.copy(lastName = value, errors = emptyList()) }
    fun onFirstMidNameChange(value: String) = _state.update { it.copy(firstMidName = value, errors = emptyList()) }
    fun onEnrollmentDateChange(value: String) = _state.update { it.copy(enrollmentDate = value, errors = emptyList()) }

    fun submit() {
        viewModelScope.launch {
            val s = _state.value
            try {
                mediator.send(
                    EditStudentCommand(
                        id = s.id,
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
