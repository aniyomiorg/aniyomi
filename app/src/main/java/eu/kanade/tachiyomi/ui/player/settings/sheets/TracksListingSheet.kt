package eu.kanade.tachiyomi.ui.player.settings.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.ui.player.settings.sheetDialogPadding

@Composable
fun TracksCatalogSheet(
    isEpisodeOnline: Boolean?,
    qualityTracks: Array<Track>,
    subtitleTracks: Array<Track>,
    audioTracks: Array<Track>,
    selectedQualityIndex: Int,
    selectedSubtitleIndex: Int,
    selectedAudioIndex: Int,
    onQualitySelected: (Int) -> Unit,
    onSubtitleSelected: (Int) -> Unit,
    onAudioSelected: (Int) -> Unit,
    onSettingsClicked: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val tabTitles = mutableListOf(
        stringResource(id = R.string.subtitle_dialog_header),
        stringResource(id = R.string.audio_dialog_header),
    )
    if (isEpisodeOnline == true) {
        tabTitles.add(0, stringResource(id = R.string.quality_dialog_header))
    }

    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = tabTitles,
        onOverflowMenuClicked = onSettingsClicked,
        overflowIcon = Icons.Outlined.Settings,
        hideSystemBars = true,
    ) { contentPadding, page ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            @Composable fun QualityTracksPage() = TracksPageBuilder(
                tracks = qualityTracks,
                selectedTrackIndex = selectedQualityIndex,
                onTrackSelected = onQualitySelected,
            )

            @Composable fun SubtitleTracksPage() = TracksPageBuilder(
                tracks = subtitleTracks,
                selectedTrackIndex = selectedSubtitleIndex,
                onTrackSelected = onSubtitleSelected,
            )

            @Composable fun AudioTracksPage() = TracksPageBuilder(
                tracks = audioTracks,
                selectedTrackIndex = selectedAudioIndex,
                onTrackSelected = onAudioSelected,
            )

            when {
                isEpisodeOnline == true && page == 0 -> QualityTracksPage()
                page == 0 || page == 1 -> SubtitleTracksPage()
                page == 2 -> AudioTracksPage()
            }
        }
    }
}

@Composable
private fun TracksPageBuilder(
    tracks: Array<Track>,
    selectedTrackIndex: Int,
    onTrackSelected: (Int) -> Unit,
) {
    var selectedIndex by remember { mutableStateOf(selectedTrackIndex) }

    val onSelected: (Int) -> Unit = { index ->
        onTrackSelected(index)
        selectedIndex = index
    }

    tracks.forEachIndexed { index, track ->
        val selected = selectedIndex == index

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onSelected(index) })
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
