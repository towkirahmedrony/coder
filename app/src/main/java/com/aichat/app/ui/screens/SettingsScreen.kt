package com.aichat.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aichat.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 🚀 NEW: Pager State for Two Tabs
    val tabs = listOf("General", "Advanced")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // 🚀 NEW: Tab Row for switching pages
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // 🚀 NEW: Swipeable Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> GeneralSettingsTab(viewModel = viewModel, settings = settings)
                    1 -> AdvancedSettingsTab(viewModel = viewModel, settings = settings)
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsTab(viewModel: SettingsViewModel, settings: com.aichat.app.data.model.AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Local AI Setup (Qwen/Colab)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(value = settings.localBaseUrl, onValueChange = { viewModel.updateSettings(settings.copy(localBaseUrl = it)) }, label = { Text("Local Base URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(value = settings.localApiKey, onValueChange = { viewModel.updateSettings(settings.copy(localApiKey = it)) }, label = { Text("Local API Key") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(value = settings.localModelName, onValueChange = { viewModel.updateSettings(settings.copy(localModelName = it)) }, label = { Text("Local Model Name") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.padding(12.dp))

        Text("Behavior Setting", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(
            value = settings.systemPrompt,
            onValueChange = { viewModel.updateSettings(settings.copy(systemPrompt = it)) },
            label = { Text("System Prompt (Regular Behavior)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 10
        )
        Spacer(modifier = Modifier.padding(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsTab(viewModel: SettingsViewModel, settings: com.aichat.app.data.model.AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Cloud AI Setup (Groq/Gemini)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(value = settings.cloudBaseUrl, onValueChange = { viewModel.updateSettings(settings.copy(cloudBaseUrl = it)) }, label = { Text("Cloud Base URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(value = settings.cloudApiKey, onValueChange = { viewModel.updateSettings(settings.copy(cloudApiKey = it)) }, label = { Text("Cloud API Key") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(value = settings.cloudModelName, onValueChange = { viewModel.updateSettings(settings.copy(cloudModelName = it)) }, label = { Text("Cloud Model Name") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.padding(12.dp))

        Text("Search Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(
            value = settings.searchApiKey,
            onValueChange = { viewModel.updateSettings(settings.copy(searchApiKey = it)) },
            label = { Text("Search API Key (Serper.dev)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(
            value = settings.searchPrompt,
            onValueChange = { viewModel.updateSettings(settings.copy(searchPrompt = it)) },
            label = { Text("Search Logic Prompt (Critical)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 15
        )

        Spacer(modifier = Modifier.padding(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Stream Response", modifier = Modifier.weight(1f))
            Switch(checked = settings.streamResponse, onCheckedChange = { viewModel.updateSettings(settings.copy(streamResponse = it)) })
        }

        Spacer(modifier = Modifier.padding(16.dp))
        Text("Theme", modifier = Modifier.padding(bottom = 8.dp))

        val themeOptions = listOf("system", "light", "dark")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            themeOptions.forEachIndexed { index, theme ->
                SegmentedButton(selected = settings.theme == theme, onClick = { viewModel.updateSettings(settings.copy(theme = theme)) }, shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)) {
                    Text(theme.replaceFirstChar { it.uppercase() })
                }
            }
        }
        Spacer(modifier = Modifier.padding(32.dp))
    }
}
