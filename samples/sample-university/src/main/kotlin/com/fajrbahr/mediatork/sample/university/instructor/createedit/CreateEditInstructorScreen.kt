package com.fajrbahr.mediatork.sample.university.instructor.createedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fajrbahr.mediatork.sample.university.common.FormBlock
import com.fajrbahr.mediatork.sample.university.common.ValidationDiv
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditInstructorScreen(viewModel: CreateEditInstructorViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    val title = if (viewModel.isEdit) "Edit" else "Create"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(
                            "Back",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Instructor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                ValidationDiv(state.errors)
                FormBlock(label = "Last Name", value = state.lastName, onValueChange = viewModel::onLastNameChange)
                FormBlock(
                    label = "First Name",
                    value = state.firstMidName,
                    onValueChange = viewModel::onFirstMidNameChange
                )
                FormBlock(label = "Hire Date", value = state.hireDate, onValueChange = viewModel::onHireDateChange)
                FormBlock(
                    label = "Office Location",
                    value = state.officeLocation,
                    onValueChange = viewModel::onOfficeLocationChange
                )
                FormBlock(
                    label = "Course IDs (comma-separated)",
                    value = state.selectedCourseIds,
                    onValueChange = viewModel::onCourseIdsChange
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (viewModel.isEdit) "Save" else "Create") }
            }
        }
    }
}
