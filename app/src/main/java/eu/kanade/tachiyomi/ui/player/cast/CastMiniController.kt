package eu.kanade.tachiyomi.ui.player.cast

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.CastManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CastMiniController(
    castManager: CastManager,
    modifier: Modifier = Modifier,
    onControllerClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    val currentMedia by castManager.currentMedia.collectAsState()
    val isPlaying by castManager.isPlaying.collectAsState()
    val currentPosition by remember(castManager) {
        derivedStateOf {
            val client = castManager.castSession?.remoteMediaClient
            client?.approximateStreamPosition ?: 0L
        }
    }
    val duration by remember(castManager) {
        derivedStateOf {
            val client = castManager.castSession?.remoteMediaClient
            client?.mediaInfo?.streamDuration ?: 0L
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val client = castManager.castSession?.remoteMediaClient
            if (client != null) {
                delay(1000)
            } else {
                delay(1000)
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (currentPosition.toFloat() / duration).coerceIn(0f, 1f),
        label = "progress",
    )

    Surface(
        modifier = modifier.clickable(
            onClick = {
                context.startActivity(
                    Intent(context, ExpandedControlsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                (context as? ComponentActivity)?.overridePendingTransition(
                    R.anim.fade_in,
                    R.anim.slide_out_up,
                )
            },
        ),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .then(
                        if (onControllerClick != null) {
                            Modifier.clickable(onClick = onControllerClick)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!currentMedia?.thumbnail.isNullOrEmpty()) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        AsyncImage(
                            model = currentMedia?.thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = currentMedia?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = currentMedia?.subtitle.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { castManager.previousVideo() },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(0.7f),
                        )
                    }

                    IconButton(
                        onClick = { castManager.seekRelative(-10) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(0.7f),
                        )
                    }

                    IconButton(
                        onClick = { castManager.playPause() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(
                        onClick = { castManager.seekRelative(10) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(0.7f),
                        )
                    }

                    IconButton(
                        onClick = { castManager.nextVideo() },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(0.7f),
                        )
                    }
                }
            }

            if (duration > 0) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
