package eu.kanade.tachiyomi.ui.player.viewer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.presentation.entries.components.DotSeparatorText
import eu.kanade.tachiyomi.util.view.setComposeContent
import `is`.xyz.mpv.MPVView.Chapter
import `is`.xyz.mpv.Utils

class CurrentChapter(
    private val view: ComposeView,
    private val onClick: () -> Unit,
) {
    private var value: Float = 0F
    private var chapters: List<Chapter> = listOf()

    fun updateCurrentChapterText(
        value: Float? = null,
        chapters: List<Chapter>? = null,
    ) {
        if (value != null) {
            this.value = value
        }
        if (chapters != null) {
            this.chapters = chapters
        }
        if (this.chapters.isEmpty()) {
            return
        }
        val chapter = this.chapters.last { it.time <= (value ?: 0F) }
        view.setComposeContent {
            CurrentChapterComposable(
                chapter = chapter,
                modifier = Modifier.clickable { onClick() },
            )
        }
    }

    @Composable
    private fun CurrentChapterComposable(
        chapter: Chapter,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            AnimatedContent(
                targetState = chapter,
                transitionSpec = {
                    if (targetState.time > initialState.time) {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false),
                    )
                },
                label = "segment",
            ) { currentSegment ->
                Row {
                    Text(
                        text = Utils.prettyTime(currentSegment.time.toInt()),
                        fontWeight = FontWeight.Bold,
                    )
                    DotSeparatorText()
                    Text(
                        text = " ${currentSegment.title}",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

