package eu.kanade.tachiyomi.ui.player.settings.sheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.settings.sheetDialogPadding
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.io.File

@Composable
fun StreamsCatalogSheet(
    isEpisodeOnline: Boolean?,
    videoStreams: PlayerViewModel.VideoStreams,
    openContentFd: (Uri) -> String?,
    onQualitySelected: (Int) -> Unit,
    onSubtitleSelected: (Int) -> Unit,
    onAudioSelected: (Int) -> Unit,
    onSettingsClicked: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val tabTitles = mutableListOf(
        stringResource(MR.strings.subtitle_dialog_header),
        stringResource(MR.strings.audio_dialog_header),
    )
    if (isEpisodeOnline == true) {
        tabTitles.add(0, stringResource(MR.strings.quality_dialog_header))
    }

    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = tabTitles.toImmutableList(),
        onOverflowMenuClicked = onSettingsClicked,
        overflowIcon = Icons.Outlined.Settings,
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical),
        ) {
            @Composable
            fun QualityTracksPage() = StreamsPageBuilder(
                externalTrackCode = null,
                stream = videoStreams.quality,
                openContentFd = openContentFd,
                onTrackSelected = onQualitySelected,
            )

            @Composable
            fun SubtitleTracksPage() = StreamsPageBuilder(
                externalTrackCode = "sub",
                stream = videoStreams.subtitle,
                openContentFd = openContentFd,
                onTrackSelected = onSubtitleSelected,
            )

            @Composable
            fun AudioTracksPage() = StreamsPageBuilder(
                externalTrackCode = "audio",
                stream = videoStreams.audio,
                openContentFd = openContentFd,
                onTrackSelected = onAudioSelected,
            )

            when (page) {
                0 -> if (isEpisodeOnline == true) QualityTracksPage() else SubtitleTracksPage()
                1 -> if (isEpisodeOnline == true) SubtitleTracksPage() else AudioTracksPage()
                2 -> if (isEpisodeOnline == true) AudioTracksPage()
            }
        }
    }
}

@Composable
private fun StreamsPageBuilder(
    externalTrackCode: String?,
    stream: PlayerViewModel.VideoStreams.Stream,
    openContentFd: (Uri) -> String?,
    onTrackSelected: (Int) -> Unit,
) {
    var tracks by remember { mutableStateOf(stream.tracks) }
    var index by remember { mutableStateOf(stream.index) }

    val onSelected: (Int) -> Unit = {
        onTrackSelected(it)
        index = it
        stream.index = it
    }

    if (externalTrackCode != null) {
        val addExternalTrack = rememberLauncherForActivityResult(
            object : ActivityResultContracts.GetContent() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = super.createIntent(context, input)
                    return if (externalTrackCode == "audio") {
                        Intent.createChooser(
                            intent,
                            context.stringResource(MR.strings.player_add_external_audio_intent),
                        )
                    } else {
                        Intent.createChooser(
                            intent,
                            context.stringResource(MR.strings.player_add_external_subtitles_intent),
                        )
                    }
                }
            },
        ) {
            if (it != null) {
                val url = it.toString()
                val path = if (url.startsWith("content://")) {
                    openContentFd(Uri.parse(url))
                } else {
                    url
                } ?: return@rememberLauncherForActivityResult
                MPVLib.command(arrayOf("$externalTrackCode-add", path, "cached"))
                val title = File(path).name
                tracks += Track(path, title)
                stream.tracks += Track(path, title)
                index = tracks.lastIndex
                stream.index = tracks.lastIndex
            }
        }

        val addTrackRes =
            if (externalTrackCode == "sub") {
                MR.strings.player_add_external_subtitles
            } else {
                MR.strings.player_add_external_audio
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { addExternalTrack.launch("*/*") })
                .padding(sheetDialogPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.padding(end = MaterialTheme.padding.extraSmall),
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(addTrackRes),
            )

            Text(
                text = stringResource(addTrackRes),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        tracks.forEachIndexed { i, track ->
            val selected = index == i

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onSelected(i) })
                    .padding(sheetDialogPadding),
            ) {
                Text(
                    text = track.lang,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                )
            }
        }
    }
}
