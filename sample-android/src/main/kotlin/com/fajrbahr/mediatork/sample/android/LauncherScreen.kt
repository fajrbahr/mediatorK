package com.fajrbahr.mediatork.sample.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    onBeforePrayerTimesClick: () -> Unit,
    onAfterPrayerTimesClick: () -> Unit,
    onAfterSuperPrayerTimesClick: () -> Unit,
    onBeforeIslamicMonthsClick: () -> Unit,
    onAfterIslamicMonthsClick: () -> Unit,
    onAfterSuperIslamicMonthsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MediatorK Sample", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Prayer Times")

            SampleCard(
                title = "Before",
                description = "Android architecture — no MediatorK\n" +
                    "RemoteDataSource  →  Repository (+ cache)  →  ViewModel\n" +
                    "ViewModel calls Repository directly",
                containerColor = Color(0xFF2C1F00),
                titleColor = Color(0xFFFFD54F),
                onClick = onBeforePrayerTimesClick,
            )

            SampleCard(
                title = "After",
                description = "Android architecture + MediatorK handlers\n" +
                    "ViewModel → Mediator → Handler (inline HTTP + cache)\n" +
                    "No repository or data source layer",
                containerColor = Color(0xFF0D2415),
                titleColor = Color(0xFF81C784),
                onClick = onAfterPrayerTimesClick,
            )

            SampleCard(
                title = "After Super",
                description = "After + pipeline behaviors\n" +
                    "Logging · Timing · Retry · Timeout · Counter · ErrorTracking\n" +
                    "Pipeline logs captured and shown inline in the screen",
                containerColor = Color(0xFF1E1032),
                titleColor = Color(0xFFCE93D8),
                onClick = onAfterSuperPrayerTimesClick,
            )

            Spacer(Modifier.height(8.dp))
            SectionHeader("Islamic Months")

            SampleCard(
                title = "Before",
                description = "Android architecture — no MediatorK\n" +
                    "RemoteDataSource  →  Repository (+ cache)  →  ViewModel\n" +
                    "ViewModel calls getIslamicMonths() directly",
                containerColor = Color(0xFF2C1F00),
                titleColor = Color(0xFFFFD54F),
                onClick = onBeforeIslamicMonthsClick,
            )

            SampleCard(
                title = "After",
                description = "Android architecture + MediatorK handlers\n" +
                    "ViewModel → Mediator → Handler (inline HTTP + cache)\n" +
                    "No repository or data source layer",
                containerColor = Color(0xFF0D2415),
                titleColor = Color(0xFF81C784),
                onClick = onAfterIslamicMonthsClick,
            )

            SampleCard(
                title = "After Super",
                description = "After + pipeline behaviors\n" +
                    "Logging · Timing · Retry · Timeout · Counter · ErrorTracking\n" +
                    "Pipeline logs captured and shown inline in the screen",
                containerColor = Color(0xFF1E1032),
                titleColor = Color(0xFFCE93D8),
                onClick = onAfterSuperIslamicMonthsClick,
            )

        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleCard(
    title: String,
    description: String,
    containerColor: Color,
    titleColor: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = titleColor.copy(alpha = 0.8f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5,
            )
        }
    }
}
