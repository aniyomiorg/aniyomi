package eu.kanade.tachiyomi.ui.player.cast.components

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import kotlinx.coroutines.delay
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun CastSheet(
    castManager: CastManager,
    viewModel: PlayerViewModel,
    onDismissRequest: () -> Unit,
) {
    val devices by castManager.availableDevices.collectAsState()
    val castState by castManager.castState.collectAsState()
    var showQualityDialog by remember { mutableStateOf(false) }
    var showPlayerDialog by remember { mutableStateOf(false) }

    // Llamar a startDeviceDiscovery siempre al inicio
    LaunchedEffect(Unit) {
        castManager.startDeviceDiscovery()
    }

    // Solo buscar dispositivos cuando estamos en estado CONNECTING
    LaunchedEffect(castState) {
        if (castState == CastManager.CastState.CONNECTING) {
            while (true) {
                delay(2000)
                castManager.startDeviceDiscovery()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        TachiyomiTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                // Dispositivos disponibles
                Text(
                    text = "Available devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.height(120.dp), // Altura más reducida para dispositivos
                ) {
                    items(devices) { device ->
                        ListItem(
                            headlineContent = { Text(device.name) },
                            leadingContent = {
                                Icon(
                                    if (device.isConnected) {
                                        Icons.Default.CastConnected
                                    } else {
                                        Icons.Default.Cast
                                    },
                                    contentDescription = null,
                                )
                            },
                            trailingContent = {
                                if (device.isConnected) {
                                    Icon(Icons.Default.Check, null)
                                }
                            },
                            modifier = Modifier.clickable {
                                if (device.isConnected) {
                                    showPlayerDialog = true
                                } else {
                                    castManager.connectToDevice(device.id)
                                }
                            },
                        )
                    }
                }

                // Cola de reproducción
                if (castState == CastManager.CastState.CONNECTED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Botones más compactos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), // Menos espacio entre botones
                    ) {
                        FilledTonalButton(
                            onClick = { showQualityDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp), // Botones más pequeños verticalmente
                        ) {
                            Text(stringResource(TLMR.strings.title_cast_quality))
                        }
                        FilledTonalButton(
                            onClick = { showPlayerDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            Text("Media Info")
                        }
                    }

                    // Queue con título
                    val queueItems by castManager.queueItems.collectAsState()
                    if (queueItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(TLMR.strings.queue),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        LazyColumn(
                            modifier = Modifier.height(120.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(queueItems) { item ->
                                QueueItemRow(item = item, castManager = castManager)
                            }
                        }
                    }
                }
            }
        }
    }

    // Mostrar diálogos solo cuando hay conexión
    if (castState == CastManager.CastState.CONNECTED) {
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
}

@Composable
private fun DeviceList(
    devices: List<CastManager.CastDevice>,
    onDeviceClick: (CastManager.CastDevice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(devices) { device ->
            DeviceItem(device = device, onClick = { onDeviceClick(device) })
        }
    }
}

@Composable
private fun DeviceItem(
    device: CastManager.CastDevice,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(device.name) },
        leadingContent = {
            Icon(
                if (device.isConnected) Icons.Default.CastConnected else Icons.Default.Cast,
                contentDescription = null,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
