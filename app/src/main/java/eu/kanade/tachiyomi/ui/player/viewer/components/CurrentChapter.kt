package eu.kanade.tachiyomi.ui.player.viewer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.view.setComposeContent
import `is`.xyz.mpv.MPVView.Chapter
import `is`.xyz.mpv.Utils
import tachiyomi.presentation.core.components.material.padding

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
        val chapter = this.chapters.last { it.time <= (value ?: 0F).coerceAtLeast(0F) }
        view.setComposeContent {
            CurrentChapterComposable(
                chapter = chapter,
                onClick = onClick,
                modifier = Modifier
                    .padding(end = MaterialTheme.padding.large)
                    .wrapContentSize(Alignment.CenterStart),
            )
        }
    }

    @Composable
    private fun CurrentChapterComposable(
        chapter: Chapter,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(25))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6F))
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
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
                label = "Chapter",
            ) { currentChapter ->
                Row(modifier = Modifier.clickable { onClick() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_video_chapter_20dp),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = MaterialTheme.padding.small)
                            .size(16.dp),
                    )
                    Text(
                        text = Utils.prettyTime(currentChapter.time.toInt()),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    currentChapter.title?.let {
                        Text(
                            text = " • ",
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                        Text(
                            text = it,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}
