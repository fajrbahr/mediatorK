package com.fajrbahr.mediatork.sample.android

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private val ErrorRed = Color(0xFFEF5350)
private val SuccessGreen = Color(0xFF66BB6A)

/**
 * @param validate Returns an error message string if [city] is invalid, null if valid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionScreen(
    subtitle: String,
    subtitleColor: Color,
    subtitleBg: Color,
    accentColor: Color,
    validate: (String) -> String?,
    onCitySelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var city by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }

    val trimmed = city.trim()
    val errorMessage: String? = if (touched && trimmed.isNotEmpty()) validate(trimmed) else null
    val isValid = trimmed.isNotEmpty() && errorMessage == null && validate(trimmed) == null

    val fieldBorderColor = when {
        touched && trimmed.isNotEmpty() && errorMessage != null -> ErrorRed
        isValid -> SuccessGreen
        else -> accentColor
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select City", fontWeight = FontWeight.Bold) },
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
            Surface(color = subtitleBg) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = subtitleColor,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Enter a city name to fetch today's prayer times.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        if (it.isNotEmpty()) touched = true
                    },
                    label = { Text("City") },
                    placeholder = { Text("e.g. London, Cairo, Dubai") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isValid) onCitySelected(trimmed) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = fieldBorderColor,
                        unfocusedBorderColor = if (isValid) SuccessGreen else accentColor.copy(alpha = 0.5f),
                        focusedLabelColor = fieldBorderColor,
                        cursorColor = fieldBorderColor,
                        errorBorderColor = ErrorRed,
                        errorLabelColor = ErrorRed,
                        errorCursorColor = ErrorRed,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        AnimatedVisibility(
                            visible = errorMessage != null || isValid,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            if (errorMessage != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        "✕  $errorMessage",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            } else if (isValid) {
                                Text(
                                    "✓  Hello, $trimmed!",
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    },
                )

                Button(
                    onClick = { if (isValid) onCitySelected(trimmed) },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        disabledContainerColor = accentColor.copy(alpha = 0.35f),
                    ),
                ) {
                    Text("Continue", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
