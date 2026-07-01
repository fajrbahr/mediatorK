package com.fajrbahr.mediatork.sample.university.course.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCourseScreen(
    viewModel: CreateCourseViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Course", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // @Html.ValidationDiv()
            ValidationDiv(state.errors)

            // @Html.FormBlock(m => m.Data.Number)
            FormBlock(
                label = "Number",
                value = state.number,
                onValueChange = viewModel::onNumberChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            // @Html.FormBlock(m => m.Data.Title)
            FormBlock(
                label = "Title",
                value = state.title,
                onValueChange = viewModel::onTitleChange,
            )

            // @Html.FormBlock(m => m.Data.Credits)
            FormBlock(
                label = "Credits",
                value = state.credits,
                onValueChange = viewModel::onCreditsChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            // @Html.FormBlock(m => m.Data.Department)
            FormBlock(
                label = "Department ID",
                value = state.departmentId.toString(),
                onValueChange = { viewModel.onDepartmentIdChange(it.toIntOrNull() ?: 0) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(Modifier.height(8.dp))

            Button(onClick = viewModel::submit, modifier = Modifier.fillMaxWidth()) {
                Text("Create")
            }
        }
    }
}
