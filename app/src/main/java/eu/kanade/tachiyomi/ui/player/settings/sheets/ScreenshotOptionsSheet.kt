package eu.kanade.tachiyomi.ui.player.settings.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import eu.kanade.tachiyomi.ui.player.settings.dialogs.PlayerDialog
import tachiyomi.presentation.core.components.material.padding
import java.io.InputStream

@Composable
fun ScreenshotOptionsSheet(
    screenModel: PlayerSettingsScreenModel,
    cachePath: String,
    onSetAsCover: (() -> InputStream) -> Unit,
    onShare: (() -> InputStream) -> Unit,
    onSave: (() -> InputStream) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }
    val showSubtitles by remember { mutableStateOf(screenModel.preferences.screenshotSubtitles()) }

    AdaptiveSheet(
        hideSystemBars = true,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(vertical = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.set_as_cover),
                    icon = Icons.Outlined.Photo,
                    onClick = { showSetCoverDialog = true },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.action_share),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        onShare { screenModel.takeScreenshot(cachePath, showSubtitles.get())!! }
                        onDismissRequest()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.action_save),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        onSave { screenModel.takeScreenshot(cachePath, showSubtitles.get())!! }
                        onDismissRequest()
                    },
                )
            }

            screenModel.ToggleableRow(
                textRes = R.string.screenshot_show_subs,
                paddingValues = PaddingValues(MaterialTheme.padding.medium),
                isChecked = showSubtitles.collectAsState().value,
                onClick = { screenModel.togglePreference { showSubtitles } },
                coloredText = true,
            )
        }
    }

    if (showSetCoverDialog) {
        PlayerDialog(
            titleRes = R.string.confirm_set_image_as_cover,
            modifier = Modifier.fillMaxWidth(fraction = 0.6F).padding(MaterialTheme.padding.medium),
            onConfirmRequest = { onSetAsCover { screenModel.takeScreenshot(cachePath, showSubtitles.get())!! } },
            onDismissRequest = { showSetCoverDialog = false },
        )
    }
}

// TODO: (Merge_Change) function is to be removed once added in merge
//  "package tachiyomi.presentation.core.components"

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
            Text(
                text = title,
                textAlign = TextAlign.Center,
            )
        }
    }
}
