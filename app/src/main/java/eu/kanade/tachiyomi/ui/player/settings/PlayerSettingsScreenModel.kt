package eu.kanade.tachiyomi.ui.player.settings

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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.player.settings.dialogs.PlayerDialog
import `is`.xyz.mpv.MPVLib
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.toggle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.InputStream

val sheetDialogPadding = PaddingValues(
    vertical = MaterialTheme.padding.small,
    horizontal = MaterialTheme.padding.medium,
)

class PlayerSettingsScreenModel(
    val preferences: PlayerPreferences = Injekt.get(),
    val decoderPreferences: DecoderPreferences = Injekt.get(),
    val gesturePreferences: GesturePreferences = Injekt.get(),
    val subtitlePreferences: SubtitlePreferences = Injekt.get(),
    val audioPreferences: AudioPreferences = Injekt.get(),
    val hasSubTracks: Boolean = true,
) : ScreenModel {

    fun togglePreference(preference: (PlayerPreferences) -> Preference<Boolean>) =
        preference(preferences).toggle()

    @Composable
    fun ToggleableRow(
        textRes: StringResource,
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
                text = stringResource(textRes),
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
            val overrideType = if (newOverrideValue) "force" else "scale"
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
                textRes = MR.strings.player_override_ass_subtitles,
                isChecked = overrideSubsASS,
                onClick = updateOverrideASS,
            )

            TextButton(onClick = { showDialog = true }) {
                Text(stringResource(MR.strings.action_reset))
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
            titleRes = MR.strings.player_reset_subtitles,
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
                    text = stringResource(MR.strings.player_subtitle_empty_warning),
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
