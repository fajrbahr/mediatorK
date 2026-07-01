package com.fajrbahr.mediatork.sample.university.instructor.createedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.instructor.domain.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.domain.GetInstructorQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateEditInstructorUiState(
    val id: Int? = null,
    val lastName: String = "",
    val firstMidName: String = "",
    val hireDate: String = "",
    val officeLocation: String = "",
    val selectedCourseIds: String = "",
    val errors: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
)

class CreateEditInstructorViewModel(
    private val mediator: Mediator,
    private val instructorId: Int? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEditInstructorUiState(isLoading = instructorId != null))
    val state: StateFlow<CreateEditInstructorUiState> = _state.asStateFlow()

    val isEdit: Boolean get() = instructorId != null

    init {
        if (instructorId != null) {
            viewModelScope.launch {
                val instructor = mediator.send(GetInstructorQuery(instructorId))
                if (instructor != null) {
                    _state.value = CreateEditInstructorUiState(
                        id = instructor.id,
                        lastName = instructor.lastName,
                        firstMidName = instructor.firstMidName,
                        hireDate = instructor.hireDate,
                        officeLocation = instructor.officeLocation ?: "",
                        selectedCourseIds = instructor.courseIds.joinToString(","),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onLastNameChange(value: String) = _state.update { it.copy(lastName = value, errors = emptyList()) }
    fun onFirstMidNameChange(value: String) = _state.update { it.copy(firstMidName = value, errors = emptyList()) }
    fun onHireDateChange(value: String) = _state.update { it.copy(hireDate = value, errors = emptyList()) }
    fun onOfficeLocationChange(value: String) = _state.update { it.copy(officeLocation = value, errors = emptyList()) }
    fun onCourseIdsChange(value: String) = _state.update { it.copy(selectedCourseIds = value, errors = emptyList()) }

    fun submit() {
        viewModelScope.launch {
            val s = _state.value
            try {
                mediator.send(
                    CreateEditInstructorCommand(
                        id = s.id,
                        lastName = s.lastName,
                        firstMidName = s.firstMidName,
                        hireDate = s.hireDate,
                        officeLocation = s.officeLocation.ifBlank { null },
                        selectedCourseIds = s.selectedCourseIds
                            .split(",")
                            .mapNotNull { it.trim().toIntOrNull() },
                    )
                )
                _state.update { it.copy(isSaved = true) }
            } catch (e: ValidationException) {
                _state.update { it.copy(errors = e.errors.map { err -> err.toString() }) }
            }
        }
    }
}
