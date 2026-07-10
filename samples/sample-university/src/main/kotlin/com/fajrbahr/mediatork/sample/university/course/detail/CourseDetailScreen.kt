package com.fajrbahr.mediatork.sample.university.course.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fajrbahr.mediatork.sample.university.common.DetailRow
import com.fajrbahr.mediatork.sample.university.common.DetailSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(viewModel: CourseDetailViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onPrimary)
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
            val model = state.model ?: return@Scaffold
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                DetailSection("Course") {
                    DetailRow("Number", model.number.toString())
                    DetailRow("Title", model.title)
                    DetailRow("Credits", model.credits.toString())
                    DetailRow("Department", model.departmentName)
                }
                if (model.enrollments.isNotEmpty()) {
                    DetailSection("Enrolled Students") {
                        model.enrollments.forEach { enrollment ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(enrollment.studentFullName, modifier = Modifier.weight(1f))
                                Text(enrollment.grade?.name ?: "No grade")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
