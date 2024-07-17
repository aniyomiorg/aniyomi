package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
@Suppress("ParameterNaming", "LongMethod")
fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    // SY -->
    onSetAsCover: (useExtraPage: Boolean) -> Unit,
    onShare: (useExtraPage: Boolean) -> Unit,
    onSave: (useExtraPage: Boolean) -> Unit,
    onShareCombined: () -> Unit,
    onSaveCombined: () -> Unit,
    hasExtraPage: Boolean,
    // SY <--
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }
    // SY -->
    var useExtraPage by remember { mutableStateOf(false) }
    // SY <--

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        // SY -->
                        if (hasExtraPage) {
                            MR.strings.action_set_first_page_cover
                        } else {
                            MR.strings.set_as_cover
                        },
                        // SY <--
                    ),
                    icon = Icons.Outlined.Photo,
                    onClick = { showSetCoverDialog = true },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        // SY -->
                        if (hasExtraPage) {
                            MR.strings.action_share_first_page
                        } else {
                            MR.strings.action_share
                        },
                        // SY <--
                    ),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        // SY -->
                        onShare(false)
                        // SY <--
                        onDismissRequest()
                    },
                )

                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(
                        // SY -->
                        if (hasExtraPage) {
                            MR.strings.action_save_first_page
                        } else {
                            MR.strings.action_save
                        },
                        // SY <--
                    ),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        // SY -->
                        onSave(false)
                        // SY <--
                        onDismissRequest()
                    },
                )
            }
            if (hasExtraPage) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(MR.strings.action_set_second_page_cover),
                        icon = Icons.Outlined.Photo,
                        onClick = {
                            showSetCoverDialog = true
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(MR.strings.action_share_second_page),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            onShare(true)
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(MR.strings.action_save_second_page),
                        icon = Icons.Outlined.Save,
                        onClick = {
                            onSave(true)
                            onDismissRequest()
                        },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(MR.strings.action_share_combined_page),
                        icon = Icons.Outlined.Share,
                        onClick = {
                            onShareCombined()
                            onDismissRequest()
                        },
                    )
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(MR.strings.action_save_combined_page),
                        icon = Icons.Outlined.Save,
                        onClick = {
                            onSaveCombined()
                            onDismissRequest()
                        },
                    )
                }
            }
    }

    if (showSetCoverDialog) {
        SetCoverDialog(
            onConfirm = {
                // SY -->
                onSetAsCover(useExtraPage)
                showSetCoverDialog = false
                useExtraPage = false
                // SY <--
            },
            onDismiss = { showSetCoverDialog = false },
        )
    }
}

@Composable
private fun SetCoverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        text = {
            Text(stringResource(MR.strings.confirm_set_image_as_cover))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
    )
}
