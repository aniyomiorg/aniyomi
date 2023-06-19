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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import eu.kanade.tachiyomi.util.Stamp
import tachiyomi.core.util.system.logcat

class Seekbar(
    private val view: ComposeView,
    private val onValueChange: (Float) -> Unit,
) {
    private var duration: Float = 1F
    private var value: Float = 0F
    private var readAheadValue: Float = 0F
    private var stamps: List<Stamp> = listOf()

    fun updateSeekbar(
        duration: Float? = null,
        value: Float? = null,
        readAheadValue: Float? = null,
        stamps: List<Stamp>? = null,
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
        if (stamps != null) {
            logcat { "stamps: $stamps" }
            this.stamps = stamps
        }

        view.setContent {
            SeekbarComposable(
                duration ?: this.duration,
                value ?: this.value,
                readAheadValue ?: this.readAheadValue,
                stamps?.toSegments(duration ?: this.duration)
                    ?: this.stamps.toSegments(duration ?: this.duration),
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
        var mutableValue by remember { mutableStateOf(value) }
        val interactionSource = remember { MutableInteractionSource() }
        val isDragging by interactionSource.collectIsDraggedAsState()
        val gap by animateDpAsState(if (isDragging) 5.dp else 2.dp, label = "gap")
        val thumbRadius by animateDpAsState(if (isDragging) 10.dp else 0.dp, label = "thumbRadius")
        val trackHeight by animateDpAsState(targetValue = if (isDragging) 6.dp else 4.dp, label = "trackHeight")
        val range = 0F..duration
        return Seeker(
            value = if (isDragging) {
                mutableValue
            } else {
                value
            },
            readAheadValue = readAheadValue,
            range = range,
            onValueChange = {
                if (isDragging) mutableValue = it
                onValueChange(it)
            },
            segments = segments,
            colors = SeekerDefaults.seekerColors(
                progressColor = MaterialTheme.colorScheme.primary,
                readAheadColor = MaterialTheme.colorScheme.inversePrimary,
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
private fun List<Stamp>.toSegments(duration: Float): List<Segment> {
    if (this.isEmpty()) return emptyList()
    val startSegments = this.map {
        Segment(
            it.skipType.getString(),
            it.interval.startTime.toFloat(),
            MaterialTheme.colorScheme.tertiary,
        )
    }
    val gapSegments = mutableListOf<Segment>()
    if (this.first().interval.startTime != 0.0) {
        gapSegments.add(
            Segment(
                "",
                0F,
            ),
        )
    }
    if (this.last().interval.endTime < duration - 1.0) {
        gapSegments.add(
            Segment(
                "",
                this.last().interval.endTime.toFloat(),
            ),
        )
    }
    for (i in 0 until this.lastIndex) {
        if (this[i].interval.endTime - this[i + 1].interval.startTime > 1) {
            gapSegments.add(
                Segment(
                    "",
                    this[i].interval.endTime.toFloat(),
                ),
            )
        }
    }
    return (startSegments + gapSegments).sortedBy { it.start }
}
