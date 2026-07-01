package com.fajrbahr.mediatork.sample.university.student.edit

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
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(viewModel: EditStudentViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit", fontWeight = FontWeight.Bold) },
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
                Text("Student", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                ValidationDiv(state.errors)
                FormBlock(label = "Last Name", value = state.lastName, onValueChange = viewModel::onLastNameChange)
                FormBlock(
                    label = "First Name",
                    value = state.firstMidName,
                    onValueChange = viewModel::onFirstMidNameChange
                )
                FormBlock(
                    label = "Enrollment Date",
                    value = state.enrollmentDate,
                    onValueChange = viewModel::onEnrollmentDateChange
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = viewModel::submit, modifier = Modifier.fillMaxWidth()) { Text("Save") }
            }
        }
    }
}
