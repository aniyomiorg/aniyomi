package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.MPVKtSpacing

@Composable
fun PlayerUpdate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(0.4f))
            .padding(vertical = MaterialTheme.MPVKtSpacing.smaller, horizontal = MaterialTheme.MPVKtSpacing.medium)
            .animateContentSize(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun TextPlayerUpdate(
    text: String,
    modifier: Modifier = Modifier
) {
    PlayerUpdate(modifier) {
        Text(text)
    }
}
