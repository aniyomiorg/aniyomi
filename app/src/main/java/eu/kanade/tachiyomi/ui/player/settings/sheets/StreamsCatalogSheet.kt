package eu.kanade.tachiyomi.ui.player.settings.sheets

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.settings.sheetDialogPadding
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import java.io.File

@Composable
fun StreamsCatalogSheet(
    isEpisodeOnline: Boolean?,
    videoStreams: PlayerViewModel.VideoStreams,
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
            @Composable fun QualityTracksPage() = StreamsPageBuilder(
                externalTrackCode = null,
                stream = videoStreams.mercedes,
                onTrackSelected = onQualitySelected,
            )

            @Composable fun SubtitleTracksPage() = StreamsPageBuilder(
                externalTrackCode = "sub",
                stream = videoStreams.bmw,
                onTrackSelected = onSubtitleSelected,
            )

            @Composable fun AudioTracksPage() = StreamsPageBuilder(
                externalTrackCode = "audio",
                stream = videoStreams.audi,
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
        val resolver = LocalContext.current.contentResolver
        val addExternalTrack = rememberLauncherForActivityResult(
            object : ActivityResultContracts.GetContent() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = super.createIntent(context, input)
                    return Intent.createChooser(intent, "Select Something")
                }
            },
        ) {
            if (it != null) {
                val url = it.toString()
                val fd = if (url.startsWith("content://")) {
                    openContentFd(Uri.parse(url), resolver)
                } else {
                    url
                } ?: return@rememberLauncherForActivityResult
                MPVLib.command(arrayOf("$externalTrackCode-add", fd, "cached"))
                val path = Utils.findRealPath(fd.substringAfter("//").toInt()) ?: fd
                tracks += Track(path, path)
                stream.tracks += Track(path, path)
                index = tracks.lastIndex
                stream.index = tracks.lastIndex
            }
        }

        val addTrackRes = if (externalTrackCode == "sub") R.string.player_add_subtitles else R.string.player_add_audio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { addExternalTrack.launch("*/*") })
                .padding(sheetDialogPadding),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(id = addTrackRes),
            )

            Text(
                text = stringResource(id = addTrackRes),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

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

private fun openContentFd(uri: Uri, resolver: ContentResolver): String? {
    if (uri.scheme != "content") return null
    val fd = try {
        val desc = resolver.openFileDescriptor(uri, "r")
        desc!!.detachFd()
    } catch (e: Exception) {
        return null
    }
    // Find out real file path and see if we can read it directly
    try {
        val path = File("/proc/self/fd/$fd").canonicalPath
        if (!path.startsWith("/proc") && File(path).canRead()) {
            ParcelFileDescriptor.adoptFd(fd).close() // we don't need that anymore
            return path
        }
    } catch (_: Exception) {}
    // Else, pass the fd to mpv
    return "fdclose://$fd"
}
