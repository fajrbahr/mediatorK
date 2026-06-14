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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fajrbahr.mediatork.sample.android.after.model.PrayerTime
import com.fajrbahr.mediatork.sample.android.after.model.TodayPrayerTimes
import com.fajrbahr.mediatork.sample.android.aftersuper.viewmodel.AfterSuperPrayerTimesViewModel
import com.fajrbahr.mediatork.sample.android.aftersuper.viewmodel.AfterSuperUiState
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterSuperPrayerTimesScreen(
    onBack: () -> Unit,
    viewModel: AfterSuperPrayerTimesViewModel = viewModel(factory = AfterSuperPrayerTimesViewModel.Factory),
) {
    BackHandler(onBack = onBack)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer Times", fontWeight = FontWeight.Bold) },
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
                    is AfterSuperUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    is AfterSuperUiState.Error -> ErrorContent(s.message, s.pipelineLogs, s.requestCount, viewModel::retry)
                    is AfterSuperUiState.Success -> PrayerTimesContent(s.prayerTimes, s.pipelineLogs, s.requestCount)
                }
            }
        }
    }
}

@Composable
private fun PrayerTimesContent(prayerTimes: TodayPrayerTimes, logs: List<String>, requestCount: Long) {
    val nextIndex = prayerTimes.prayers.indexOfFirst { !it.isPast() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(prayerTimes.gregorianDate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(prayerTimes.hijriDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text("London · UTC", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
            }
        }
        if (nextIndex >= 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Next Prayer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(prayerTimes.prayers[nextIndex].name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Text(prayerTimes.prayers[nextIndex].time.substringBefore(" "), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        item {
            Text("All Prayers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(vertical = 4.dp))
        }
        items(prayerTimes.prayers) { prayer ->
            PrayerRow(prayer, isNext = prayerTimes.prayers.indexOf(prayer) == nextIndex)
        }
        item { PipelineLogsCard(logs, requestCount) }
    }
}

@Composable
private fun PrayerRow(prayer: PrayerTime, isNext: Boolean) {
    val isPast = prayer.isPast()
    val alpha = if (isPast) 0.38f else 1f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (isNext) 2.dp else 0.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(if (isPast) Color.Gray.copy(0.25f) else MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                Text(prayer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            }
            Text(prayer.time.substringBefore(" "), style = MaterialTheme.typography.bodyLarge, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal, color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(0.38f) else MaterialTheme.colorScheme.primary)
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

private fun PrayerTime.isPast(): Boolean {
    val parts = time.substringBefore(" ").split(":")
    if (parts.size != 2) return false
    val h = parts[0].toIntOrNull() ?: return false
    val m = parts[1].toIntOrNull() ?: return false
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    return h < cal.get(Calendar.HOUR_OF_DAY) || (h == cal.get(Calendar.HOUR_OF_DAY) && m <= cal.get(Calendar.MINUTE))
}
