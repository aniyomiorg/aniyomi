package eu.kanade.tachiyomi.ui.player.cast.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import eu.kanade.tachiyomi.ui.player.CastManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import logcat.LogPriority
import logcat.logcat
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.concurrent.TimeUnit

@SuppressLint("DefaultLocale")
@Composable
fun ExpandedControllerScreen(
    castManager: CastManager,
    castContext: CastContext,
    onBackPressed: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var client by remember { mutableStateOf<RemoteMediaClient?>(null) }
    var mediaStatus by remember { mutableStateOf<MediaStatus?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var thumbnail by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var showTracksDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showSubtitleSettings by remember { mutableStateOf(false) }
    var subtitleSettings by remember { mutableStateOf(SubtitleSettings()) }

    val mediaCallback = remember {
        object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                client?.let { safeClient ->
                    isPlaying = safeClient.isPlaying
                    currentPosition = safeClient.approximateStreamPosition
                    duration = safeClient.mediaInfo?.streamDuration ?: 0L
                    thumbnail = safeClient.mediaInfo?.metadata?.images?.firstOrNull()?.url?.toString()
                    title = safeClient.mediaInfo?.metadata?.getString("title").orEmpty()
                    subtitle = safeClient.mediaInfo?.metadata?.getString("subtitle").orEmpty()
                    mediaStatus = safeClient.mediaStatus
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        castContext.sessionManager.currentCastSession?.remoteMediaClient?.let { newClient ->
                            client = newClient
                            newClient.registerCallback(mediaCallback)
                            mediaCallback.onStatusUpdated()
                        }
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR) { "Error initializing cast client: ${e.message}" }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    client?.unregisterCallback(mediaCallback)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            client?.unregisterCallback(mediaCallback)
        }
    }

    LaunchedEffect(client) {
        while (isActive) {
            currentPosition = client?.approximateStreamPosition ?: 0L
            delay(1000)
        }
    }

    // UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = mediaStatus?.mediaInfo?.metadata?.getString(MediaMetadata.KEY_TITLE)
                                ?: title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = mediaStatus?.mediaInfo?.metadata?.getString(MediaMetadata.KEY_SUBTITLE)
                                ?: subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSubtitleSettings = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(TLMR.strings.cast_subtitle_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { showDisconnectDialog = true },
                        modifier = Modifier.animateContentSize(),
                    ) {
                        val isConnected = castContext.sessionManager.currentCastSession?.isConnected == true
                        Icon(
                            imageVector = if (isConnected) {
                                Icons.Default.CastConnected
                            } else {
                                Icons.Default.Cast
                            },
                            contentDescription = "Cast",
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isConnected) 1f else 0.7f,
                            ),
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = if (isConnected) 1.1f else 1f
                                    scaleY = if (isConnected) 1.1f else 1f
                                }
                                .animateContentSize(),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        ),
                ) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatDuration(currentPosition),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = { client?.seek(it.toLong()) },
                                valueRange = 0f..duration.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                                ),
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = MaterialTheme.shapes.small,
                                            ),
                                    )
                                },
                                track = {
                                    SliderDefaults.Track(
                                        modifier = Modifier.height(4.dp),
                                        sliderState = it,
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme
                                                .onSurface.copy(alpha = 0.32f),
                                        ),
                                    )
                                },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledIconButton(
                            onClick = {
                                client?.let { remoteClient ->
                                    val queue = remoteClient.mediaQueue
                                    val currentItemId = remoteClient.currentItem?.itemId ?: return@let
                                    val currentIndex = (0 until queue.itemCount).find {
                                        queue.getItemAtIndex(it)?.itemId == currentItemId
                                    } ?: return@let

                                    if (currentIndex > 0) {
                                        remoteClient.queueJumpToItem(
                                            queue.getItemAtIndex(currentIndex - 1)?.itemId ?: return@let,
                                            null,
                                        )
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(TLMR.strings.cast_previous_video),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Rewind
                        FilledIconButton(
                            onClick = { client?.seek(currentPosition - 30000) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay30,
                                contentDescription = stringResource(TLMR.strings.cast_rewind_30s),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        FilledIconButton(
                            onClick = { if (isPlaying) client?.pause() else client?.play() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) {
                                    stringResource(TLMR.strings.cast_pause)
                                } else {
                                    stringResource(TLMR.strings.cast_play)
                                },
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        FilledIconButton(
                            onClick = { client?.seek(currentPosition + 30000) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward30,
                                contentDescription = stringResource(TLMR.strings.cast_forward_30s),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                client?.let { remoteClient ->
                                    val queue = remoteClient.mediaQueue
                                    val currentItemId = remoteClient.currentItem?.itemId ?: return@let
                                    val currentIndex = (0 until queue.itemCount).find {
                                        queue.getItemAtIndex(it)?.itemId == currentItemId
                                    } ?: return@let

                                    if (currentIndex < queue.itemCount - 1) {
                                        remoteClient.queueJumpToItem(
                                            queue.getItemAtIndex(currentIndex + 1)?.itemId ?: return@let,
                                            null,
                                        )
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(TLMR.strings.cast_next_video),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        FilledTonalIconButton(
                            onClick = { showTracksDialog = true },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = stringResource(TLMR.strings.cast_tracks),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }

                        VolumeControl(castContext = castContext)
                    }
                }
            }
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text(stringResource(TLMR.strings.cast_end_session)) },
            text = { Text(stringResource(TLMR.strings.cast_end_session_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        castContext.sessionManager.endCurrentSession(true)
                        showDisconnectDialog = false
                    },
                ) {
                    Text(stringResource(TLMR.strings.cast_end_session_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(stringResource(TLMR.strings.cast_end_session_cancel))
                }
            },
        )
    }

    if (showTracksDialog) {
        TracksSelectionDialog(
            client = client,
            onDismiss = { showTracksDialog = false },
        )
    }

    if (showSubtitleSettings) {
        SubtitleSettingsDialog(
            onDismissRequest = { showSubtitleSettings = false },
            initialSettings = castManager.getDefaultSubtitleSettings(),
            onSettingsChanged = { newSettings ->
                subtitleSettings = newSettings
                castManager.applySubtitleSettings(newSettings)
            },
        )
    }
}

@Composable
private fun VolumeControl(
    castContext: CastContext,
    modifier: Modifier = Modifier,
) {
    var showVolumeSlider by remember { mutableStateOf(false) }
    var volume by remember {
        mutableFloatStateOf(
            castContext.sessionManager.currentCastSession?.volume?.toFloat() ?: 1f,
        )
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier.align(Alignment.Center),
        ) {
            FilledTonalIconButton(
                onClick = { showVolumeSlider = !showVolumeSlider },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Icon(
                    imageVector = when {
                        volume == 0f -> Icons.Default.VolumeOff
                        volume < 0.5f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = stringResource(TLMR.strings.cast_volume),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            if (showVolumeSlider) {
                Popup(
                    onDismissRequest = { showVolumeSlider = false },
                    offset = IntOffset(0, -100),
                ) {
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .height(64.dp),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 3.dp,
                        shadowElevation = 6.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    val newVolume = (volume - 0.1f).coerceIn(0f, 1f)
                                    volume = newVolume
                                    castContext.sessionManager.currentCastSession?.volume = newVolume.toDouble()
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeDown,
                                    contentDescription = stringResource(TLMR.strings.cast_volume_down),
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Slider(
                                    value = volume,
                                    onValueChange = { newVolume ->
                                        volume = newVolume
                                        castContext.sessionManager.currentCastSession?.volume = newVolume.toDouble()
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                                    ),
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newVolume = (volume + 0.1f).coerceIn(0f, 1f)
                                    volume = newVolume
                                    castContext.sessionManager.currentCastSession?.volume = newVolume.toDouble()
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = stringResource(TLMR.strings.cast_volume_up),
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Text(
                                text = "${(volume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(48.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TracksSelectionDialog(
    client: RemoteMediaClient?,
    onDismiss: () -> Unit,
) {
    val tracks = remember(client) {
        client?.mediaStatus?.mediaInfo?.mediaTracks?.toList() ?: emptyList()
    }
    var activeTrackIds by remember(client) {
        mutableStateOf(client?.mediaStatus?.activeTrackIds?.toSet() ?: emptySet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(TLMR.strings.cast_tracks)) },
        text = {
            LazyColumn {
                val subtitleTracks = tracks.filter { it.type == MediaTrack.TYPE_TEXT }
                val audioTracks = tracks.filter { it.type == MediaTrack.TYPE_AUDIO }

                if (subtitleTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(TLMR.strings.cast_subtitles),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        TrackItem(
                            track = null,
                            name = stringResource(TLMR.strings.cast_no_subtitles),
                            isSelected = !activeTrackIds.any { id ->
                                tracks.find { it.id == id }?.type == MediaTrack.TYPE_TEXT
                            },
                            onSelected = { selected ->
                                if (selected) {
                                    activeTrackIds = activeTrackIds.filter { id ->
                                        tracks.find { it.id == id }?.type != MediaTrack.TYPE_TEXT
                                    }.toSet()
                                    client?.setActiveMediaTracks(activeTrackIds.toLongArray())
                                }
                            },
                        )
                    }
                    items(subtitleTracks) { track ->
                        TrackItem(
                            track = track,
                            name = track.name ?: "Unknown",
                            isSelected = track.id in activeTrackIds,
                            onSelected = { selected ->
                                val newTrackIds = if (selected) {
                                    activeTrackIds.plus(track.id)
                                } else {
                                    activeTrackIds.minus(track.id)
                                }
                                activeTrackIds = newTrackIds
                                client?.setActiveMediaTracks(newTrackIds.toLongArray())
                            },
                        )
                    }
                }

                if (audioTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(TLMR.strings.cast_audio_tracks),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(audioTracks) { track ->
                        TrackItem(
                            track = track,
                            name = track.name ?: "Unknown",
                            isSelected = track.id in activeTrackIds,
                            onSelected = { selected ->
                                if (selected) {
                                    val otherAudioTracks = audioTracks.filter { it.id != track.id }
                                    val newTrackIds = activeTrackIds
                                        .minus(otherAudioTracks.map { it.id }.toSet())
                                        .plus(track.id)
                                    activeTrackIds = newTrackIds
                                    client?.setActiveMediaTracks(newTrackIds.toLongArray())
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(TLMR.strings.cast_close))
            }
        },
    )
}

@Composable
private fun TrackItem(
    track: MediaTrack?,
    name: String,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatDuration(milliseconds: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}
