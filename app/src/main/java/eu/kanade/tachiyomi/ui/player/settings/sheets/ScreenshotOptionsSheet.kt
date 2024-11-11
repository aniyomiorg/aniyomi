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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import eu.kanade.tachiyomi.ui.player.settings.dialogs.PlayerDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
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
    val showSubtitles by remember { mutableStateOf(screenModel.subtitlePreferences.screenshotSubtitles()) }

    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(vertical = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(MR.strings.set_as_cover),
                    icon = Icons.Outlined.Photo,
                    onClick = { showSetCoverDialog = true },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(MR.strings.action_share),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        onShare { screenModel.takeScreenshot(cachePath, showSubtitles.get())!! }
                        onDismissRequest()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(MR.strings.action_save),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        onSave { screenModel.takeScreenshot(cachePath, showSubtitles.get())!! }
                        onDismissRequest()
                    },
                )
            }

            if (screenModel.hasSubTracks) {
                screenModel.ToggleableRow(
                    textRes = MR.strings.screenshot_show_subs,
                    paddingValues = PaddingValues(MaterialTheme.padding.medium),
                    isChecked = showSubtitles.collectAsState().value,
                    onClick = { screenModel.togglePreference { showSubtitles } },
                    coloredText = true,
                )
            }
        }
    }

    if (showSetCoverDialog) {
        PlayerDialog(
            titleRes = MR.strings.confirm_set_image_as_cover,
            modifier = Modifier.fillMaxWidth(fraction = 0.6F).padding(MaterialTheme.padding.medium),
            onConfirmRequest = {
                onSetAsCover {
                    screenModel.takeScreenshot(
                        cachePath,
                        showSubtitles.get(),
                    )!!
                }
            },
            onDismissRequest = { showSetCoverDialog = false },
        )
    }
}
