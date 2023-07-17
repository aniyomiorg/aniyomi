package eu.kanade.tachiyomi.ui.player.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.R

@Composable
fun SubtitleSettingsDialog(
    onDismissRequest: () -> Unit,
) {
    PlayerDialog(
        titleRes = R.string.player_hwdec_dialog_title,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            Row {
                IconButton(onClick = {  }) {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = null,
                    )
                }

                IconButton(onClick = {  }) {
                    Icon(
                        imageVector = Icons.Outlined.FormatBold,
                        contentDescription = null,
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Outlined.FormatItalic,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
