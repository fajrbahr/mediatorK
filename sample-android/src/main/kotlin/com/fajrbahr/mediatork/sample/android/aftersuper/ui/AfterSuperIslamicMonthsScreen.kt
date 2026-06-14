package com.fajrbahr.mediatork.sample.android.aftersuper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fajrbahr.mediatork.sample.android.after.model.IslamicMonth
import com.fajrbahr.mediatork.sample.android.aftersuper.viewmodel.AfterSuperIslamicMonthsViewModel
import com.fajrbahr.mediatork.sample.android.aftersuper.viewmodel.AfterSuperMonthsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterSuperIslamicMonthsScreen(
    onBack: () -> Unit,
    viewModel: AfterSuperIslamicMonthsViewModel = viewModel(factory = AfterSuperIslamicMonthsViewModel.Factory),
) {
    BackHandler(onBack = onBack)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Islamic Months", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(color = Color(0xFF1E1032)) {
                Text(
                    "After Super  —  Logging · Timing · Retry · Timeout · Counter · ErrorTracking",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFCE93D8),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val s = state) {
                    is AfterSuperMonthsUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    is AfterSuperMonthsUiState.Error -> ErrorContent(s.message, s.pipelineLogs, s.requestCount, viewModel::retry)
                    is AfterSuperMonthsUiState.Success -> MonthsContent(s.months, s.pipelineLogs, s.requestCount)
                }
            }
        }
    }
}

@Composable
private fun MonthsContent(months: List<IslamicMonth>, logs: List<String>, requestCount: Long) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Hijri Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
            Text("12 months of the Islamic lunar calendar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 12.dp))
        }
        items(months) { month -> MonthRow(month) }
        item { PipelineLogsCard(logs, requestCount) }
    }
}

@Composable
private fun MonthRow(month: IslamicMonth) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(month.number.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(month.nameEn, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(month.nameAr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun PipelineLogsCard(logs: List<String>, requestCount: Long = 0L) {
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Pipeline Logs", style = MaterialTheme.typography.labelMedium, color = Color(0xFFCBA6F7), fontWeight = FontWeight.Bold)
                if (requestCount > 0) Text("sent ${requestCount}×", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA6E3A1))
            }
            Spacer(Modifier.height(4.dp))
            if (logs.isEmpty()) {
                Text("no logs captured", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6C7086), fontFamily = FontFamily.Monospace)
            } else {
                logs.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCDD6F4), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, logs: List<String>, requestCount: Long, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Failed to load", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Button(onClick = onRetry) { Text("Retry") }
        PipelineLogsCard(logs, requestCount)
    }
}
