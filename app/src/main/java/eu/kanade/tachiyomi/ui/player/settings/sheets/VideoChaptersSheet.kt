package eu.kanade.tachiyomi.ui.player.settings.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.player.settings.sheetDialogPadding
import `is`.xyz.mpv.Utils
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt
import `is`.xyz.mpv.MPVView.Chapter as VideoChapter

@Composable
fun VideoChaptersSheet(
    timePosition: Int,
    videoChapters: List<VideoChapter>,
    onVideoChapterSelected: (VideoChapter, String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var currentTimePosition by remember { mutableStateOf(timePosition) }

    AdaptiveSheet(
        hideSystemBars = true,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .padding(MaterialTheme.padding.medium)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(MR.strings.chapter_dialog_header),
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp,
            )

            val currentChapter = videoChapters.last { it.time <= currentTimePosition }

            videoChapters.forEach { videoChapter ->
                val videoChapterTime = videoChapter.time.roundToInt()
                val videoChapterName = if (videoChapter.title.isNullOrBlank()) {
                    Utils.prettyTime(videoChapterTime)
                } else {
                    "${videoChapter.title} (${Utils.prettyTime(videoChapterTime)})"
                }

                val selected = videoChapter == currentChapter

                val onClick = {
                    currentTimePosition = videoChapter.time.roundToInt()
                    onVideoChapterSelected(videoChapter, videoChapterName)
                    onDismissRequest()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(sheetDialogPadding),
                ) {
                    Text(
                        text = videoChapterName,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                }
            }
        }
    }
}
