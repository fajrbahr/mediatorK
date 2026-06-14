package com.fajrbahr.mediatork.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.fajrbahr.mediatork.sample.android.after.ui.AfterIslamicMonthsScreen
import com.fajrbahr.mediatork.sample.android.after.ui.AfterPrayerTimesScreen
import com.fajrbahr.mediatork.sample.android.before.ui.BeforeIslamicMonthsScreen
import com.fajrbahr.mediatork.sample.android.before.ui.BeforePrayerTimesScreen
import com.fajrbahr.mediatork.sample.android.aftersuper.ui.AfterSuperIslamicMonthsScreen
import com.fajrbahr.mediatork.sample.android.aftersuper.ui.AfterSuperPrayerTimesScreen
import com.fajrbahr.mediatork.sample.android.ui.theme.PrayerTimesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrayerTimesTheme {
                AppRoot()
            }
        }
    }
}

private enum class Screen {
    Launcher,
    BeforePrayerTimes,
    AfterPrayerTimes,
    AfterSuperPrayerTimes,
    BeforeIslamicMonths,
    AfterIslamicMonths,
    AfterSuperIslamicMonths,
}

@Composable
private fun AppRoot() {
    var screen by rememberSaveable { mutableStateOf(Screen.Launcher) }
    when (screen) {
        Screen.Launcher -> LauncherScreen(
            onBeforePrayerTimesClick = { screen = Screen.BeforePrayerTimes },
            onAfterPrayerTimesClick = { screen = Screen.AfterPrayerTimes },
            onAfterSuperPrayerTimesClick = { screen = Screen.AfterSuperPrayerTimes },
            onBeforeIslamicMonthsClick = { screen = Screen.BeforeIslamicMonths },
            onAfterIslamicMonthsClick = { screen = Screen.AfterIslamicMonths },
            onAfterSuperIslamicMonthsClick = { screen = Screen.AfterSuperIslamicMonths },
        )
        Screen.BeforePrayerTimes -> BeforePrayerTimesScreen(onBack = { screen = Screen.Launcher })
        Screen.AfterPrayerTimes -> AfterPrayerTimesScreen(onBack = { screen = Screen.Launcher })
        Screen.AfterSuperPrayerTimes -> AfterSuperPrayerTimesScreen(onBack = { screen = Screen.Launcher })
        Screen.BeforeIslamicMonths -> BeforeIslamicMonthsScreen(onBack = { screen = Screen.Launcher })
        Screen.AfterIslamicMonths -> AfterIslamicMonthsScreen(onBack = { screen = Screen.Launcher })
        Screen.AfterSuperIslamicMonths -> AfterSuperIslamicMonthsScreen(onBack = { screen = Screen.Launcher })
    }
}
