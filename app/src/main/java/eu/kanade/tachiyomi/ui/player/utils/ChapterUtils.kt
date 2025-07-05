package eu.kanade.tachiyomi.ui.player.utils

import androidx.compose.ui.graphics.Color
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.TimeStamp
import eu.kanade.tachiyomi.ui.player.controls.components.IndexedSegment
import tachiyomi.i18n.aniyomi.AYMR
import kotlin.math.abs

class ChapterUtils {
    companion object {
        fun ChapterType.getStringRes(): StringResource? = when (this) {
            ChapterType.Opening -> AYMR.strings.player_chapter_type_opening
            ChapterType.Ending -> AYMR.strings.player_chapter_type_ending
            ChapterType.Recap -> AYMR.strings.player_chapter_type_recap
            ChapterType.MixedOp -> AYMR.strings.player_chapter_type_mixedop
            ChapterType.Other -> null
        }

        fun mergeChapters(
            currentChapters: List<IndexedSegment>,
            stamps: List<TimeStamp>,
            duration: Int? = null,
        ): List<IndexedSegment> {
            val sortedStamps = stamps.sortedBy { it.start }
            val chapters = sortedStamps.mapIndexed { i, it ->
                val startTime = if (i == 0 && it.start < 1.0) {
                    0.0
                } else {
                    it.start
                }
                val startChapter = IndexedSegment(
                    index = -2, // Index -2 is used to indicate that this is an external chapter
                    name = it.name,
                    start = startTime.toFloat(),
                    color = if (it.type == ChapterType.Other) Color.Unspecified else Color(0xFFD8BBDF),
                    chapterType = it.type,
                )
                val nextStart = sortedStamps.getOrNull(i + 1)?.start
                val isNotLastChapter = abs(it.end - (duration?.toDouble() ?: -2.0)) > 1.0
                val isNotAdjacent = nextStart == null || (abs(it.end - nextStart) > 1.0)
                if (isNotLastChapter && isNotAdjacent) {
                    val endChapter = IndexedSegment(
                        index = -1,
                        name = "",
                        start = it.end.toFloat(),
                    )
                    return@mapIndexed listOf(startChapter, endChapter)
                } else {
                    listOf(startChapter)
                }
            }.flatten()
            val playerChapters = currentChapters.filter { playerChapter ->
                chapters.none { chapter ->
                    abs(chapter.start - playerChapter.start) < 1.0 && chapter.index == -2
                }
            }.map {
                IndexedSegment(it.name, it.start, it.color, chapterType = it.chapterType)
            }.sortedBy { it.start }.mapIndexed { i, it ->
                if (i == 0 && it.start < 1.0) {
                    IndexedSegment(
                        it.name,
                        0.0f,
                        index = it.index,
                        chapterType = it.chapterType,
                    )
                } else {
                    it
                }
            }
            val filteredChapters = chapters.filter { chapter ->
                playerChapters.none { playerChapter ->
                    abs(chapter.start - playerChapter.start) < 1.0 && chapter.index != -2
                }
            }
            val startChapter = if ((playerChapters + filteredChapters).isNotEmpty() &&
                playerChapters.none { it.start == 0.0f } &&
                filteredChapters.none { it.start == 0.0f }
            ) {
                listOf(
                    IndexedSegment(
                        index = -1,
                        name = "",
                        start = 0.0f,
                    ),
                )
            } else {
                emptyList()
            }

            val combined = (startChapter + playerChapters + filteredChapters).sortedBy { it.start }

            // Remove any adjacent "empty" chapters
            return combined.filterIndexed { index, segment ->
                if (index == 0) {
                    true
                } else {
                    val previousSegment = combined[index - 1]
                    !(segment.name.isEmpty() && previousSegment.name.isEmpty())
                }
            }
        }
    }
}
