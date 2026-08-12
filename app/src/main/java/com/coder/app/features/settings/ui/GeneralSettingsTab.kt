package com.coder.app.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.coder.app.features.settings.ui.components.SettingsGroup
import com.coder.app.features.settings.ui.components.SettingsTextField
import com.coder.app.features.settings.ui.components.validateUrl
import com.coder.app.features.settings.ui.viewmodel.GeneralUiState
import com.coder.app.features.settings.ui.viewmodel.SettingsFieldUpdater

@Composable
fun GeneralSettingsTab(
    uiState: GeneralUiState,
    onFieldChange: SettingsFieldUpdater,
    onFieldFocusLost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroup(
            title = "Local AI Setup",
            subtitle = "Qwen · Colab · self-hosted endpoint",
            icon = Icons.Filled.Memory
        ) {
            SettingsTextField(
                value = uiState.localBaseUrl,
                onValueChange = { onFieldChange { s -> s.copy(localBaseUrl = it) } },
                label = "Local Base URL",
                validator = ::validateUrl,
                onFocusLost = onFieldFocusLost
            )
            SettingsTextField(
                value = uiState.localApiKey,
                onValueChange = { onFieldChange { s -> s.copy(localApiKey = it) } },
                label = "Local API Key",
                isPassword = true,
                onFocusLost = onFieldFocusLost
            )
            SettingsTextField(
                value = uiState.localModelName,
                onValueChange = { onFieldChange { s -> s.copy(localModelName = it) } },
                label = "Local Model Name",
                imeAction = ImeAction.Done,
                onFocusLost = onFieldFocusLost
            )
        }

        SettingsGroup(
            title = "Behavior",
            subtitle = "Default system instructions",
            icon = Icons.Filled.Tune
        ) {
            SettingsTextField(
                value = uiState.systemPrompt,
                onValueChange = { onFieldChange { s -> s.copy(systemPrompt = it) } },
                label = "System Prompt (Regular Behavior)",
                minLines = 3,
                maxLines = 10,
                imeAction = ImeAction.Default,
                onFocusLost = onFieldFocusLost
            )
        }
    }
}
