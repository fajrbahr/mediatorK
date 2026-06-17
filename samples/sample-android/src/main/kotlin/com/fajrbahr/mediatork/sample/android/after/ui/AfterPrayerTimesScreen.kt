package com.fajrbahr.mediatork.sample.android.after.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fajrbahr.mediatork.sample.android.after.model.PrayerTime
import com.fajrbahr.mediatork.sample.android.after.model.TodayPrayerTimes
import com.fajrbahr.mediatork.sample.android.after.viewmodel.AfterPrayerTimesViewModel
import com.fajrbahr.mediatork.sample.android.after.viewmodel.AfterUiState
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterPrayerTimesScreen(
    city: String,
    onBack: () -> Unit,
    viewModel: AfterPrayerTimesViewModel = viewModel(
        key = city,
        factory = AfterPrayerTimesViewModel.factory(city),
    ),
) {
    BackHandler(onBack = onBack)

    // AFTER: collectAsStateWithLifecycle() — stops collecting when UI moves to background
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
            AfterBanner()
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (val s = state) {
                    is AfterUiState.Loading -> CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )

                    is AfterUiState.Error -> ErrorContent(s.message, viewModel::retry)
                    is AfterUiState.Success -> PrayerTimesContent(s.prayerTimes, city)
                }
            }
        }
    }
}

@Composable
private fun AfterBanner() {
    Surface(color = Color(0xFF0D2415)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "After  —  Android architecture applied",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF81C784),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PrayerTimesContent(prayerTimes: TodayPrayerTimes, city: String) {
    val nextIndex = prayerTimes.prayers.indexOfFirst { !it.isPast() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { DateHeader(prayerTimes, city) }
        if (nextIndex >= 0) {
            item {
                NextPrayerCard(prayerTimes.prayers[nextIndex])
                Spacer(Modifier.height(4.dp))
            }
        }
        item {
            Text(
                "All Prayers",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        items(prayerTimes.prayers) { prayer ->
            PrayerRow(prayer, isNext = prayerTimes.prayers.indexOf(prayer) == nextIndex)
        }
    }
}

@Composable
private fun DateHeader(prayerTimes: TodayPrayerTimes, city: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(prayerTimes.gregorianDate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            prayerTimes.hijriDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "$city · UTC",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun NextPrayerCard(prayer: PrayerTime) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Next Prayer",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    prayer.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    prayer.time.substringBefore(" "),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(prayer: PrayerTime, isNext: Boolean) {
    val isPast = prayer.isPast()
    val textAlpha = if (isPast) 0.38f else 1f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 2.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(8.dp).background(
                        if (isPast) Color.Gray.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp)
                    )
                )
                Text(
                    prayer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                )
            }
            Text(
                prayer.time.substringBefore(" "),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Failed to load", style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Button(onClick = onRetry) { Text("Retry") }
    }
}

private fun PrayerTime.isPast(): Boolean {
    val parts = time.substringBefore(" ").split(":")
    if (parts.size != 2) return false
    val prayerHour = parts[0].toIntOrNull() ?: return false
    val prayerMinute = parts[1].toIntOrNull() ?: return false
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    return prayerHour < cal.get(Calendar.HOUR_OF_DAY) ||
            (prayerHour == cal.get(Calendar.HOUR_OF_DAY) && prayerMinute <= cal.get(Calendar.MINUTE))
}
