package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.player.components.PlayerSheet
import eu.kanade.tachiyomi.ui.player.PlayerViewModel.VideoTrack
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.MPVKtSpacing
import tachiyomi.presentation.core.i18n.stringResource


@Composable
fun <T> GenericTracksSheet(
    tracks: ImmutableList<T>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    track: @Composable (T) -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    PlayerSheet(onDismissRequest) {
        Column(modifier) {
            header()
            LazyColumn {
                items(tracks) {
                    track(it)
                }
                item {
                    footer()
                }
            }
        }
    }
}

@Composable
fun AddTrackRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.smaller),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxHeight()
                .weight(1f)
                .padding(start = MaterialTheme.MPVKtSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.smaller),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                null,
                modifier = Modifier.size(32.dp),
            )
            Text(title)
        }
        actions()
    }
}

@Composable
fun getTrackTitle(track: VideoTrack): String {
    return when {
        track.id == -1 -> {
            track.name
        }

        track.language.isNullOrBlank() && track.name.isNotBlank() -> {
            stringResource(MR.strings.player_sheets_track_title_wo_lang, track.id, track.name)
        }

        !track.language.isNullOrBlank() && track.name.isNotBlank() -> {
            stringResource(MR.strings.player_sheets_track_title_w_lang, track.id, track.name, track.language)
        }

        !track.language.isNullOrBlank() && track.name.isBlank() -> {
            stringResource(MR.strings.player_sheets_track_lang_wo_title, track.id, track.language)
        }

        else -> stringResource(MR.strings.player_sheets_track_title_wo_lang, track.id, track.name)
    }
}
