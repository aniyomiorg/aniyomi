package eu.kanade.presentation.more.settings.screen.player.editor.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import eu.kanade.presentation.more.settings.screen.player.editor.FileCreationResult
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun FileCreateDialog(
    initialName: String?,
    fileExtension: String,
    onDismissRequest: () -> Unit,
    isValid: (String, String?) -> FileCreationResult,
    onConfirm: (String) -> Unit,
) {
    val initialTextValue = initialName ?: "file.$fileExtension"
    var fileName by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialTextValue,
                selection = TextRange(
                    0,
                    initialTextValue.indexOfLast { it == '.' }.takeUnless { it == -1 } ?: initialTextValue.length,
                ),
            ),
        )
    }
    val result = remember(fileName.text) {
        isValid(fileName.text, initialName)
    }

    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = result is FileCreationResult.Success,
                onClick = {
                    onConfirm(fileName.text)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(if (initialName == null) MR.strings.action_add else MR.strings.action_edit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(
                text = stringResource(
                    if (initialName ==
                        null
                    ) {
                        AYMR.strings.editor_create_file
                    } else {
                        AYMR.strings.editor_edit_file
                    },
                ),
            )
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text(text = stringResource(AYMR.strings.editor_filename)) },
                supportingText = {
                    when (result) {
                        is FileCreationResult.Failure -> {
                            Text(text = stringResource(result.stringRes))
                        }
                        FileCreationResult.Success -> {}
                    }
                },
                isError = result is FileCreationResult.Failure,
                singleLine = true,
            )
        },
    )

    LaunchedEffect(focusRequester) {
        // TODO: https://issuetracker.google.com/issues/204502668
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun FileDeleteDialog(
    name: String,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(AYMR.strings.editor_delete_file))
        },
        text = {
            Text(text = stringResource(AYMR.strings.editor_delete_file_confirmation, name))
        },
    )
}

@Composable
fun UnsavedChangesDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.label_warning))
        },
        text = {
            Text(text = stringResource(AYMR.strings.editor_unsaved_progress))
        },
    )
}
