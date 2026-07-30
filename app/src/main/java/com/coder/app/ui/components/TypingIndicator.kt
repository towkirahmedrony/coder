package com.coder.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing_indicator")
    val alpha1 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), "t1")
    val alpha2 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, delayMillis = 150, easing = FastOutSlowInEasing), RepeatMode.Reverse), "t2")
    val alpha3 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse), "t3")

    Row(
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(12.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingCircle(alpha1)
        Spacer(Modifier.width(4.dp))
        TypingCircle(alpha2)
        Spacer(Modifier.width(4.dp))
        TypingCircle(alpha3)
    }
}

@Composable 
private fun TypingCircle(alpha: Float) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
    )
}
