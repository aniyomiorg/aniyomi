/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ChaptersSheet(
    chapters: ImmutableList<Segment>,
    currentChapter: Segment,
    onClick: (Segment) -> Unit,
    onDismissRequest: () -> Unit,
    dismissSheet: Boolean,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        tracks = chapters,
        header = {
            TrackSheetTitle(
                title = stringResource(AYMR.strings.player_sheets_chapters_title),
                modifier = modifier.padding(top = MaterialTheme.padding.small),
            )
        },
        track = {
            ChapterTrack(
                chapter = it,
                index = chapters.indexOf(it),
                selected = currentChapter == it,
                onClick = { onClick(it) },
            )
        },
        onDismissRequest = onDismissRequest,
        dismissEvent = dismissSheet,
        modifier = modifier,
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
            .padding(vertical = MaterialTheme.padding.small, horizontal = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(AYMR.strings.player_sheets_track_title_wo_lang, index + 1, chapter.name),
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
