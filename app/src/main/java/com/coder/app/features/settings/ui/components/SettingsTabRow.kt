package com.coder.app.features.settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SettingsTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    pageOffsetFraction: Float,
    onTabSelected: (Int) -> Unit
) {
    var rowWidthPx by remember { mutableIntStateOf(0) }
    val tabWidthPx = if (tabs.isNotEmpty() && rowWidthPx > 0) rowWidthPx / tabs.size else 0
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width }
    ) {
        TabRow(
            selectedTabIndex = selectedIndex,
            indicator = {}, // Custom indicator handles animation smoothly
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

        // Custom animated indicator using continuous drag fraction
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
