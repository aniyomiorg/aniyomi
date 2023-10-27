package eu.kanade.tachiyomi.ui.player.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.settings.dialogs.PlayerDialog
import eu.kanade.tachiyomi.util.preference.toggle
import `is`.xyz.mpv.MPVLib
import tachiyomi.core.preference.Preference
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.InputStream

val sheetDialogPadding = PaddingValues(vertical = MaterialTheme.padding.small, horizontal = MaterialTheme.padding.medium)

class PlayerSettingsScreenModel(
    val preferences: PlayerPreferences = Injekt.get(),
    private val hasSubTracks: Boolean = true,
) : ScreenModel {

    fun togglePreference(preference: (PlayerPreferences) -> Preference<Boolean>) =
        preference(preferences).toggle()

    @Composable
    fun ToggleableRow(
        @StringRes textRes: Int,
        paddingValues: PaddingValues = sheetDialogPadding,
        isChecked: Boolean,
        onClick: () -> Unit,
        coloredText: Boolean = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(paddingValues),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = textRes),
                color = if (coloredText) MaterialTheme.colorScheme.primary else Color.Unspecified,
                style = MaterialTheme.typography.titleSmall,
            )
            Switch(
                checked = isChecked,
                onCheckedChange = null,
            )
        }
    }

    @Composable
    fun OverrideSubtitlesSwitch(
        content: @Composable () -> Unit,
    ) {
        val overrideSubsASS by preferences.overrideSubsASS().collectAsState()

        val updateOverrideASS = {
            val newOverrideValue = togglePreference(PlayerPreferences::overrideSubsASS)
            val overrideType = if (newOverrideValue) "force" else "no"
            MPVLib.setPropertyString("sub-ass-override", overrideType)
        }

        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            ResetSubtitlesDialog(onDismissRequest = { showDialog = false })
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NoSubtitlesWarning()

            content()

            ToggleableRow(
                textRes = R.string.player_override_ass_subtitles,
                isChecked = overrideSubsASS,
                onClick = updateOverrideASS,
            )

            TextButton(onClick = { showDialog = true }) {
                Text(stringResource(id = R.string.action_reset))
            }
        }
    }

    @Composable
    private fun ResetSubtitlesDialog(
        onDismissRequest: () -> Unit,
    ) {
        val resetSubtitles = {
            with(preferences) {
                overrideSubsASS().delete()

                subtitleFontSize().delete()
                boldSubtitles().delete()
                italicSubtitles().delete()

                textColorSubtitles().delete()
                borderColorSubtitles().delete()
                backgroundColorSubtitles().delete()
            }
        }

        PlayerDialog(
            titleRes = R.string.player_reset_subtitles,
            hideSystemBars = true,
            modifier = Modifier
                .fillMaxWidth(fraction = 0.6F)
                .padding(MaterialTheme.padding.medium),
            onConfirmRequest = resetSubtitles,
            onDismissRequest = onDismissRequest,
        )
    }

    @Composable
    fun NoSubtitlesWarning() {
        if (!hasSubTracks) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(id = R.string.player_subtitle_empty_warning),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }

    fun takeScreenshot(cachePath: String, showSubtitles: Boolean): InputStream? {
        val filename = cachePath + "/${System.currentTimeMillis()}_mpv_screenshot_tmp.png"
        val subtitleFlag = if (showSubtitles) "subtitles" else "video"

        MPVLib.command(arrayOf("screenshot-to-file", filename, subtitleFlag))
        val tempFile = File(filename).takeIf { it.exists() } ?: return null
        val newFile = File("$cachePath/mpv_screenshot.png")

        newFile.delete()
        tempFile.renameTo(newFile)
        return newFile.takeIf { it.exists() }?.inputStream()
    }
}
