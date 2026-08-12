package com.coder.app.features.settings.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coder.app.features.settings.ui.components.SaveStatusIndicator
import com.coder.app.features.settings.ui.components.SettingsTabRow
import com.coder.app.core.common.OnLifecycleEvent
import com.coder.app.features.settings.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val generalState by viewModel.generalUiState.collectAsStateWithLifecycle()
    val advancedState by viewModel.advancedUiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // অপটিমাইজেশন ১: ViewModel-এর ফাংশনগুলোকে remember দিয়ে ক্যাশ (Cache) করা হলো
    val onFieldChange = remember(viewModel) { viewModel::updateField }
    val onFieldFocusLost = remember(viewModel) { viewModel::flushNow }

    // Flush any pending debounced write when the app backgrounds
    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_STOP) {
            onFieldFocusLost()
        }
    }

    // Flush when this screen leaves composition entirely
    DisposableEffect(onFieldFocusLost) {
        onDispose {
            onFieldFocusLost()
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is SettingsViewModel.SaveState.Saved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val tabs = remember { listOf("General", "Advanced") }
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { SaveStatusIndicator(saveState = saveState) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            SettingsTabRow(
                tabs = tabs,
                selectedIndex = pagerState.currentPage,
                pageOffsetFraction = pagerState.currentPageOffsetFraction,
                onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 8.dp
            ) { page ->
                when (page) {
                    0 -> GeneralSettingsTab(
                        uiState = generalState,
                        onFieldChange = onFieldChange,
                        onFieldFocusLost = onFieldFocusLost
                    )
                    1 -> AdvancedSettingsTab(
                        uiState = advancedState,
                        onFieldChange = onFieldChange,
                        onFieldFocusLost = onFieldFocusLost
                    )
                }
            }
        }
    }
}
