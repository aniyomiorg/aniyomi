package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.MPVKtSpacing
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ChaptersSheet(
    chapters: ImmutableList<Segment>,
    currentChapter: Segment,
    onClick: (Segment) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        chapters,
        track = {
            ChapterTrack(
                it,
                index = chapters.indexOf(it),
                selected = currentChapter == it,
                onClick = { onClick(it) },
            )
        },
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .padding(vertical = MaterialTheme.MPVKtSpacing.medium),
    )
}

@Composable
fun ChapterTrack(
    chapter: Segment,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.MPVKtSpacing.smaller, horizontal = MaterialTheme.MPVKtSpacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(MR.strings.player_sheets_track_title_wo_lang, index + 1, chapter.name),
            fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            Utils.prettyTime(chapter.start.toInt()),
            fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
        )
    }
}
