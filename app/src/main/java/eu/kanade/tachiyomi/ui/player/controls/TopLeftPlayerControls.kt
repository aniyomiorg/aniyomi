package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import tachiyomi.presentation.core.components.material.MPVKtSpacing

@Composable
fun TopLeftPlayerControls(
    animeTitle: String,
    mediaTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.small)
    ) {
        ControlsButton(
            icon = Icons.AutoMirrored.Default.ArrowBack,
            onClick = onBackClick,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(-MaterialTheme.MPVKtSpacing.extraSmall)
        ) {
            Text(
                animeTitle,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                mediaTitle,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Preview
@Composable
fun TopLeftPlayerControlsPreview() {
    TopLeftPlayerControls(
        animeTitle = "Bleach",
        mediaTitle = "Episode 1 - A Shinigami is born",
        onBackClick = {}
    )
}
