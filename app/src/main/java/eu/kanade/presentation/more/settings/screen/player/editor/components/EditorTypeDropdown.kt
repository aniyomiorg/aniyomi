package eu.kanade.presentation.more.settings.screen.player.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.screen.player.editor.EditorListType
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EditorTypeDropDown(
    type: EditorListType,
    values: ImmutableList<EditorListType>,
    onSelect: (EditorListType) -> Unit,
) {
    val isDropDownExpanded = remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    isDropDownExpanded.value = !isDropDownExpanded.value
                },
            ) {
                Text(text = stringResource(type.stringRes))
                Icon(if (isDropDownExpanded.value) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, null)
            }

            DropdownMenu(
                expanded = isDropDownExpanded.value,
                onDismissRequest = {
                    isDropDownExpanded.value = false
                },
            ) {
                values.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(item.stringRes))
                        },
                        onClick = {
                            isDropDownExpanded.value = false
                            onSelect(item)
                        },
                    )
                }
            }
        }
    }
}
