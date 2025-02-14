import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.cast.components.CastPlayerDialog
import eu.kanade.tachiyomi.ui.player.cast.components.CastQualityDialog
import eu.kanade.tachiyomi.ui.player.cast.components.QueueItemRow
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun CastControlSheet(
    castManager: CastManager,
    viewModel: PlayerViewModel,
    onDismissRequest: () -> Unit,
) {
    var showQualityDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showPlayerDialog by remember { mutableStateOf(false) }
    val queueItems by castManager.queueItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        // Current media info
        ListItem(
            headlineContent = { Text(viewModel.mediaTitle.value) },
            leadingContent = { Icon(Icons.Default.PlayArrow, null) },
            modifier = Modifier.clickable { showPlayerDialog = true },
        )

        Divider()

        // Botones de control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FilledTonalButton(onClick = { showQualityDialog = true }) {
                Text(stringResource(TLMR.strings.title_cast_quality))
            }
            FilledTonalButton(onClick = { showQueueDialog = true }) {
                Text(stringResource(TLMR.strings.cast_queue_title))
            }
        }

        // Cola de reproducción
        if (queueItems.isNotEmpty()) {
            Text(
                text = stringResource(TLMR.strings.queue),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn {
                items(queueItems) { item ->
                    QueueItemRow(
                        item = item,
                        castManager = castManager,
                    )
                }
            }
        }
    }

    // Diálogos
    if (showQualityDialog) {
        CastQualityDialog(
            viewModel = viewModel,
            castManager = castManager,
            onDismiss = { showQualityDialog = false },
        )
    }

    if (showPlayerDialog) {
        CastPlayerDialog(
            castManager = castManager,
            onDismiss = { showPlayerDialog = false },
        )
    }
}
