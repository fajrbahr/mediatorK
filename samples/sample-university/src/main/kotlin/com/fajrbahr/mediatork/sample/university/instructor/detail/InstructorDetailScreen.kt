package com.fajrbahr.mediatork.sample.university.instructor.detail

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.fajrbahr.mediatork.sample.university.common.DetailRow
import com.fajrbahr.mediatork.sample.university.common.DetailSection
import com.fajrbahr.mediatork.sample.university.instructor.detail.InstructorDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorDetailScreen(viewModel: InstructorDetailViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.Bold) },
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
            val instructor = state.instructor ?: return@Scaffold
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                DetailSection("Instructor") {
                    DetailRow("Last Name", instructor.lastName)
                    DetailRow("First Name", instructor.firstMidName)
                    DetailRow("Hire Date", instructor.hireDate)
                    DetailRow("Office", instructor.officeLocation ?: "None")
                    DetailRow("Courses", instructor.courseIds.joinToString(", ") { "#$it" }.ifEmpty { "None" })
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::delete,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
