package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.vivvvek.seeker.Segment
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import eu.kanade.tachiyomi.ui.player.controls.components.CurrentChapter
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun BottomLeftPlayerControls(
    playbackSpeed: Float,
    currentChapter: Segment?,
    onLockControls: () -> Unit,
    onCycleRotation: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onOpenSheet: (Sheets) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlsButton(
            Icons.Default.LockOpen,
            onClick = onLockControls,
        )
        ControlsButton(
            icon = Icons.Default.ScreenRotation,
            onClick = onCycleRotation,
        )
        ControlsButton(
            text = stringResource(MR.strings.player_speed, playbackSpeed),
            onClick = {
                val newSpeed = if (playbackSpeed >= 2) 0.25f else playbackSpeed + 0.25f
                onPlaybackSpeedChange(newSpeed)
                playerPreferences.playerSpeed().set(newSpeed)
            },
            onLongClick = { onOpenSheet(Sheets.PlaybackSpeed) },
        )
        AnimatedVisibility(
            currentChapter != null && playerPreferences.showCurrentChapter().get(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CurrentChapter(
                chapter = currentChapter!!,
                onClick = { onOpenSheet(Sheets.Chapters) },
            )
        }
    }
}
