package com.aichat.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
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

    // Flush any pending debounced write when the app backgrounds or this
    // screen leaves composition. Narrows, but does not fully eliminate,
    // the process-death data-loss window.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.flushNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flushNow()
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is SettingsViewModel.SaveState.Saved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

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
                        onFieldChange = viewModel::updateField,
                        onFieldFocusLost = viewModel::flushNow
                    )
                    1 -> AdvancedSettingsTab(
                        uiState = advancedState,
                        onFieldChange = viewModel::updateField,
                        onFieldFocusLost = viewModel::flushNow
                    )
                }
            }
        }
    }
}

/**
 * Custom tab indicator whose position is driven directly by the Pager's
 * continuous drag fraction (no spring layered on top of an already-animated
 * drag value) — it tracks the swipe 1:1, then settles instantly when the
 * pager settles. Uses the lambda `offset {}` overload, which is a
 * layout-phase-only placement (no extra recomposition per frame).
 */
@Composable
private fun SettingsTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    pageOffsetFraction: Float,
    onTabSelected: (Int) -> Unit
) {
    var rowWidthPx by remember { mutableStateOf(0) }
    val tabWidthPx = if (tabs.isNotEmpty() && rowWidthPx > 0) rowWidthPx / tabs.size else 0
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width }
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            indicator = {},
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (tabWidthPx > 0) {
            val offsetPx = (tabWidthPx * (selectedIndex + pageOffsetFraction)).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetPx, 0) }
                    .align(Alignment.BottomStart)
                    .width(with(density) { tabWidthPx.toDp() })
                    .height(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
            )
        }
    }
}

@Composable
private fun SaveStatusIndicator(saveState: SettingsViewModel.SaveState) {
    Row(
        modifier = Modifier.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = saveState,
            label = "save-status",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
            when (state) {
                is SettingsViewModel.SaveState.Saving ->
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                is SettingsViewModel.SaveState.Saved ->
                    Text("Saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                is SettingsViewModel.SaveState.Error ->
                    Text("Save failed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                SettingsViewModel.SaveState.Idle -> Unit
            }
        }
    }
}
