package eu.kanade.tachiyomi.ui.player.cast.components

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun CastQualityDialog(
    viewModel: PlayerViewModel,
    castManager: CastManager,
    onDismiss: () -> Unit,
) {
    TachiyomiTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(TLMR.strings.title_cast_quality)) },
            text = {
                LazyColumn {
                    items(viewModel.videoList.value.size) { index ->
                        val video = viewModel.videoList.value[index]
                        val isSelected = index == viewModel.selectedVideoIndex.value
                        QualityListItem(
                            quality = video.quality,
                            isSelected = isSelected,
                            onClick = {
                                viewModel.setVideoIndex(index)
                                castManager.loadRemoteMedia()
                                onDismiss()
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = LocalContext.current.getString(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun QualityListItem(
    quality: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = quality,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun QueueItemRow(
    item: MediaQueueItem,
    castManager: CastManager,
) {
    val metadata = item.media?.metadata
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp), // Padding reducido
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Usa weight para el texto
            Text(
                text = metadata?.getString(MediaMetadata.KEY_TITLE) ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, // Limita a una línea
            )
            Text(
                text = metadata?.getString(MediaMetadata.KEY_SUBTITLE) ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1, // Limita a una línea
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            IconButton(
                onClick = { castManager.moveQueueItem(item.itemId, 0) },
                modifier = Modifier.size(32.dp), // Botones más pequeños
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "Move to top", Modifier.size(20.dp))
            }
            IconButton(
                onClick = { castManager.moveQueueItem(item.itemId, Int.MAX_VALUE) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Move to bottom", Modifier.size(20.dp))
            }
            IconButton(
                onClick = { castManager.removeQueueItem(item.itemId) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.Close, "Remove", Modifier.size(20.dp))
            }
        }
    }
}
