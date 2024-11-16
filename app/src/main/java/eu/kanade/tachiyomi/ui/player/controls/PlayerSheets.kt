package eu.kanade.tachiyomi.ui.player.controls

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import dev.vivvvek.seeker.Segment
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.ui.player.Decoder
import eu.kanade.tachiyomi.ui.player.Panels
import eu.kanade.tachiyomi.ui.player.PlayerViewModel.VideoTrack
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.AudioTracksSheet
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.ChaptersSheet
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.DecodersSheet
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.MoreSheet
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.PlaybackSpeedSheet
import eu.kanade.tachiyomi.ui.player.controls.components.sheets.SubtitlesSheet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun PlayerSheets(
    sheetShown: Sheets,

    // subtitles sheet
    subtitles: ImmutableList<VideoTrack>,
    selectedSubtitles: ImmutableList<Int>,
    onAddSubtitle: (Uri) -> Unit,
    onSelectSubtitle: (Int) -> Unit,

    // audio sheet
    audioTracks: ImmutableList<VideoTrack>,
    selectedAudio: Int,
    onAddAudio: (Uri) -> Unit,
    onSelectAudio: (Int) -> Unit,

    // chapters sheet
    chapter: Segment?,
    chapters: ImmutableList<Segment>,
    onSeekToChapter: (Int) -> Unit,

    // Decoders sheet
    decoder: Decoder,
    onUpdateDecoder: (Decoder) -> Unit,

    // Speed sheet
    speed: Float,
    onSpeedChange: (Float) -> Unit,

    // More sheet
    sleepTimerTimeRemaining: Int,
    onStartSleepTimer: (Int) -> Unit,
    // TODO(customButtons)
    // buttons: ImmutableList<CustomButtonEntity>,

    onOpenPanel: (Panels) -> Unit,
    onDismissRequest: () -> Unit,
) {
    when (sheetShown) {
        Sheets.None -> {}
        Sheets.SubtitleTracks -> {
            val subtitlesPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) {
                if (it == null) return@rememberLauncherForActivityResult
                onAddSubtitle(it)
            }
            SubtitlesSheet(
                tracks = subtitles.toImmutableList(),
                selectedTracks = selectedSubtitles,
                onSelect = onSelectSubtitle,
                onAddSubtitle = { subtitlesPicker.launch(arrayOf("*/*")) },
                onOpenSubtitleSettings = { onOpenPanel(Panels.SubtitleSettings) },
                onOpenSubtitleDelay = { onOpenPanel(Panels.SubtitleDelay) },
                onDismissRequest = onDismissRequest,
            )
        }

        Sheets.AudioTracks -> {
            val audioPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) {
                if (it == null) return@rememberLauncherForActivityResult
                onAddAudio(it)
            }
            AudioTracksSheet(
                tracks = audioTracks,
                selectedId = selectedAudio,
                onSelect = onSelectAudio,
                onAddAudioTrack = { audioPicker.launch(arrayOf("*/*")) },
                onOpenDelayPanel = { onOpenPanel(Panels.AudioDelay) },
                onDismissRequest,
            )
        }

        Sheets.Chapters -> {
            if (chapter == null) return
            ChaptersSheet(
                chapters,
                currentChapter = chapter,
                onClick = { onSeekToChapter(chapters.indexOf(chapter)) },
                onDismissRequest,
            )
        }

        Sheets.Decoders -> {
            DecodersSheet(
                selectedDecoder = decoder,
                onSelect = onUpdateDecoder,
                onDismissRequest,
            )
        }

        Sheets.More -> {
            MoreSheet(
                remainingTime = sleepTimerTimeRemaining,
                onStartTimer = onStartSleepTimer,
                onDismissRequest = onDismissRequest,
                onEnterFiltersPanel = { onOpenPanel(Panels.VideoFilters) },
                // TODO(customButtons)
                // customButtons = buttons,
            )
        }

        Sheets.PlaybackSpeed -> {
            PlaybackSpeedSheet(
                speed,
                onSpeedChange = onSpeedChange,
                onDismissRequest = onDismissRequest
            )
        }
    }
}
