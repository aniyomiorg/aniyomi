package eu.kanade.tachiyomi.ui.player.viewer.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import eu.kanade.tachiyomi.util.view.setComposeContent
import `is`.xyz.mpv.MPVView.Chapter

class Seekbar(
    private val view: ComposeView,
    private val onValueChange: (Float, Boolean) -> Unit,
    private val onValueChangeFinished: (Float) -> Unit,
) {
    private var duration: Float = 1F
    private var value: Float = 0F
    private var readAheadValue: Float = 0F
    private var chapters: List<Chapter> = listOf()
    private var isDragging: Boolean = false

    fun updateSeekbar(
        duration: Float? = null,
        value: Float? = null,
        readAheadValue: Float? = null,
        chapters: List<Chapter>? = null,
    ) {
        if (duration != null) {
            this.duration = duration
        }
        if (value != null) {
            this.value = value
        }
        if (readAheadValue != null) {
            this.readAheadValue = readAheadValue
        }
        if (chapters != null) {
            this.chapters = chapters
        }

        view.setComposeContent {
            SeekbarComposable(
                duration ?: this.duration,
                value ?: this.value,
                readAheadValue ?: this.readAheadValue,
                chapters?.toSegments()
                    ?: this.chapters.toSegments(),
            )
        }
    }

    @Composable
    private fun SeekbarComposable(
        duration: Float,
        value: Float,
        readAheadValue: Float,
        segments: List<Segment>,
    ) {
        val range = 0F..duration
        val validSegments = segments.filter { it.start in range }
        var mutableValue by remember { mutableStateOf(value) }
        val interactionSource = remember { MutableInteractionSource() }
        val isDragging by interactionSource.collectIsDraggedAsState()
        val gap by animateDpAsState(if (isDragging) 5.dp else 2.dp, label = "gap")
        val thumbRadius by animateDpAsState(if (isDragging) 10.dp else 8.dp, label = "thumbRadius")
        val trackHeight by animateDpAsState(targetValue = if (isDragging) 6.dp else 4.dp, label = "trackHeight")
        return Seeker(
            value = value,
            readAheadValue = readAheadValue,
            range = range,
            onValueChangeFinished = {
                if (this.isDragging) {
                    onValueChangeFinished(mutableValue)
                    this.isDragging = false
                }
            },
            onValueChange = {
                mutableValue = it
                if (isDragging) {
                    val wasDragging = this.isDragging
                    this.isDragging = true
                    onValueChange(mutableValue, wasDragging)
                } else {
                    onValueChangeFinished(mutableValue)
                }
            },
            segments = validSegments,
            colors = SeekerDefaults.seekerColors(
                progressColor = MaterialTheme.colorScheme.primary,
                readAheadColor = MaterialTheme.colorScheme.onSurface,
                trackColor = MaterialTheme.colorScheme.surface,
                thumbColor = MaterialTheme.colorScheme.primary,
            ),
            dimensions = SeekerDefaults.seekerDimensions(
                trackHeight = trackHeight,
                gap = gap,
                thumbRadius = thumbRadius,
            ),
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun List<Chapter>.toSegments(): List<Segment> {
    return this.sortedBy { it.time }.map {
        // Color for AniSkip chapters
        val color = if (it.index == -2) {
            MaterialTheme.colorScheme.tertiary
        } else {
            Color.Unspecified
        }
        Segment(
            it.title ?: "",
            it.time.toFloat(),
            color,
        )
    }
}
