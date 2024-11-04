package eu.kanade.presentation.more.settings.widget

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private enum class State {
    CHECKED,
    INVERSED,
    UNCHECKED,
}

@SuppressLint("ComposeParameterOrder")
@Composable
fun <T> TriStateListDialog(
    title: String,
    message: String? = null,
    items: List<T>,
    initialChecked: List<T>,
    initialInversed: List<T>,
    itemLabel: @Composable (T) -> String,
    onDismissRequest: () -> Unit,
    onValueChanged: (newIncluded: List<T>, newExcluded: List<T>) -> Unit,
) {
    val selected = remember {
        items
            .map {
                when (it) {
                    in initialChecked -> State.CHECKED
                    in initialInversed -> State.INVERSED
                    else -> State.UNCHECKED
                }
            }
            .toMutableStateList()
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                if (message != null) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                Box {
                    val listState = rememberLazyListState()
                    LazyColumn(state = listState) {
                        itemsIndexed(items = items) { index, item ->
                            val state = selected[index]
                            Row(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        selected[index] = when (state) {
                                            State.UNCHECKED -> State.CHECKED
                                            State.CHECKED -> State.INVERSED
                                            State.INVERSED -> State.UNCHECKED
                                        }
                                    }
                                    .defaultMinSize(minHeight = 48.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    modifier = Modifier.padding(end = 20.dp),
                                    imageVector = when (state) {
                                        State.UNCHECKED -> Icons.Rounded.CheckBoxOutlineBlank
                                        State.CHECKED -> Icons.Rounded.CheckBox
                                        State.INVERSED -> Icons.Rounded.DisabledByDefault
                                    },
                                    tint = if (state == State.UNCHECKED) {
                                        LocalContentColor.current
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    contentDescription = stringResource(
                                        when (state) {
                                            State.UNCHECKED -> MR.strings.disabled
                                            State.CHECKED -> MR.strings.disabled
                                            State.INVERSED -> MR.strings.disabled
                                        },
                                    ),
                                )
                                Text(text = itemLabel(item))
                            }
                        }
                    }

                    if (listState.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                    if (listState.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val included = items.mapIndexedNotNull { index, category ->
                        if (selected[index] == State.CHECKED) category else null
                    }
                    val excluded = items.mapIndexedNotNull { index, category ->
                        if (selected[index] == State.INVERSED) category else null
                    }
                    onValueChanged(included, excluded)
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}
