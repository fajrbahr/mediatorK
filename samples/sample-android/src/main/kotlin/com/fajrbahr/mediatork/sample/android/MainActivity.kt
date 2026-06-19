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
import androidx.compose.ui.graphics.Color
import com.fajrbahr.mediatork.sample.android.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.android.after.ui.AfterIslamicMonthsScreen
import com.fajrbahr.mediatork.sample.android.after.ui.AfterPrayerTimesScreen
import com.fajrbahr.mediatork.sample.android.aftersuper.domain.GetPrayerTimesValidator
import com.fajrbahr.mediatork.sample.android.before.ui.BeforeIslamicMonthsScreen
import com.fajrbahr.mediatork.sample.android.before.ui.BeforePrayerTimesScreen
import com.fajrbahr.mediatork.sample.android.aftersuper.ui.AfterSuperIslamicMonthsScreen
import com.fajrbahr.mediatork.sample.android.aftersuper.ui.AfterSuperPrayerTimesScreen
import com.fajrbahr.mediatork.sample.android.ui.theme.PrayerTimesTheme
import com.fajrbahr.mediatork.validator.ValidationResult

private fun basicCityValidate(city: String): String? = when {
    city.length < 2 -> "City must be at least 2 characters"
    !city.all { it.isLetter() || it.isWhitespace() || it == '-' } -> "City must contain only letters"
    else -> null
}

private val afterSuperValidator = GetPrayerTimesValidator()

private fun afterSuperCityValidate(city: String): String? {
    val result = afterSuperValidator.validate(GetPrayerTimesRequest(city = city))
    return if (result is ValidationResult.Invalid) result.errors.firstOrNull()?.toString() else null
}

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
    BeforeCitySelection,
    BeforePrayerTimes,
    AfterCitySelection,
    AfterPrayerTimes,
    AfterSuperCitySelection,
    AfterSuperPrayerTimes,
    BeforeIslamicMonths,
    AfterIslamicMonths,
    AfterSuperIslamicMonths,
}

@Composable
private fun AppRoot() {
    var screen by rememberSaveable { mutableStateOf(Screen.Launcher) }
    var city by rememberSaveable { mutableStateOf("") }

    when (screen) {
        Screen.Launcher -> LauncherScreen(
            onBeforePrayerTimesClick = { screen = Screen.BeforeCitySelection },
            onAfterPrayerTimesClick = { screen = Screen.AfterCitySelection },
            onAfterSuperPrayerTimesClick = { screen = Screen.AfterSuperCitySelection },
            onBeforeIslamicMonthsClick = { screen = Screen.BeforeIslamicMonths },
            onAfterIslamicMonthsClick = { screen = Screen.AfterIslamicMonths },
            onAfterSuperIslamicMonthsClick = { screen = Screen.AfterSuperIslamicMonths },
        )

        Screen.BeforeCitySelection -> CitySelectionScreen(
            subtitle = "Before  —  ViewModel calls Repository directly (no MediatorK)",
            subtitleColor = Color(0xFFFFD54F),
            subtitleBg = Color(0xFF2C1F00),
            accentColor = Color(0xFFFFD54F),
            validate = ::basicCityValidate,
            onCitySelected = { city = it; screen = Screen.BeforePrayerTimes },
            onBack = { screen = Screen.Launcher },
        )

        Screen.AfterCitySelection -> CitySelectionScreen(
            subtitle = "After  —  ViewModel → Mediator → Handler (no repository)",
            subtitleColor = Color(0xFF81C784),
            subtitleBg = Color(0xFF0D2415),
            accentColor = Color(0xFF81C784),
            validate = ::basicCityValidate,
            onCitySelected = { city = it; screen = Screen.AfterPrayerTimes },
            onBack = { screen = Screen.Launcher },
        )

        Screen.AfterSuperCitySelection -> CitySelectionScreen(
            subtitle = "After Super  —  ValidationBehavior validates city before the handler runs",
            subtitleColor = Color(0xFFCE93D8),
            subtitleBg = Color(0xFF1E1032),
            accentColor = Color(0xFFCE93D8),
            validate = ::afterSuperCityValidate,
            onCitySelected = { city = it; screen = Screen.AfterSuperPrayerTimes },
            onBack = { screen = Screen.Launcher },
        )

        Screen.BeforePrayerTimes -> BeforePrayerTimesScreen(
            city = city,
            onBack = { screen = Screen.Launcher },
        )

        Screen.AfterPrayerTimes -> AfterPrayerTimesScreen(
            city = city,
            onBack = { screen = Screen.Launcher },
        )

        Screen.AfterSuperPrayerTimes -> AfterSuperPrayerTimesScreen(
            city = city,
            onBack = { screen = Screen.Launcher },
        )

        Screen.BeforeIslamicMonths -> BeforeIslamicMonthsScreen(onBack = { screen = Screen.Launcher })
        Screen.AfterIslamicMonths -> AfterIslamicMonthsScreen(onBack = { screen = Screen.Launcher })
        Screen.AfterSuperIslamicMonths -> AfterSuperIslamicMonthsScreen(onBack = { screen = Screen.Launcher })
    }
}
