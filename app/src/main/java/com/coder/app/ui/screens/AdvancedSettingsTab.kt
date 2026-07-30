package com.coder.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.coder.app.ui.components.SettingsGroup
import com.coder.app.ui.components.SettingsTextField
import com.coder.app.ui.components.validateUrl
import com.coder.app.viewmodel.AdvancedUiState
import com.coder.app.viewmodel.SettingsFieldUpdater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsTab(
    uiState: AdvancedUiState,
    onFieldChange: SettingsFieldUpdater,
    onFieldFocusLost: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroup(
            title = "Cloud AI Setup",
            subtitle = "Groq · Gemini · hosted endpoint",
            icon = Icons.Filled.Cloud
        ) {
            SettingsTextField(
                value = uiState.cloudBaseUrl,
                onValueChange = { onFieldChange { s -> s.copy(cloudBaseUrl = it) } },
                label = "Cloud Base URL",
                validator = ::validateUrl,
                onFocusLost = onFieldFocusLost
            )
            SettingsTextField(
                value = uiState.cloudApiKey,
                onValueChange = { onFieldChange { s -> s.copy(cloudApiKey = it) } },
                label = "Cloud API Key",
                isPassword = true,
                onFocusLost = onFieldFocusLost
            )
            SettingsTextField(
                value = uiState.cloudModelName,
                onValueChange = { onFieldChange { s -> s.copy(cloudModelName = it) } },
                label = "Cloud Model Name",
                imeAction = ImeAction.Done,
                onFocusLost = onFieldFocusLost
            )
        }

        SettingsGroup(
            title = "Search & Response",
            subtitle = "Web search behavior and streaming",
            icon = Icons.Filled.Search
        ) {
            SettingsTextField(
                value = uiState.searchApiKey,
                onValueChange = { onFieldChange { s -> s.copy(searchApiKey = it) } },
                label = "Search API Key (Serper.dev)",
                isPassword = true,
                onFocusLost = onFieldFocusLost
            )
            SettingsTextField(
                value = uiState.searchPrompt,
                onValueChange = { onFieldChange { s -> s.copy(searchPrompt = it) } },
                label = "Search Logic Prompt (Critical)",
                minLines = 4,
                maxLines = 15,
                imeAction = ImeAction.Default,
                onFocusLost = onFieldFocusLost
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stream Response", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Show tokens as they arrive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.streamResponse,
                    onCheckedChange = { checked ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFieldChange { it.copy(streamResponse = checked) }
                    }
                )
            }
        }

        SettingsGroup(
            title = "Appearance",
            subtitle = "App-wide theme",
            icon = Icons.Filled.Palette
        ) {
            val themeOptions = listOf("system", "light", "dark")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                themeOptions.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = uiState.theme == theme,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFieldChange { it.copy(theme = theme) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                    ) {
                        Text(theme.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }
}
