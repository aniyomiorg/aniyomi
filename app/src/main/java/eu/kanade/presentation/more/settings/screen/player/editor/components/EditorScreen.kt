package eu.kanade.presentation.more.settings.screen.player.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.IntegrationInstructions
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.FloatingActionAddButton
import eu.kanade.presentation.more.settings.screen.player.editor.EditorListItem
import eu.kanade.presentation.more.settings.screen.player.editor.EditorListType
import eu.kanade.presentation.more.settings.screen.player.editor.EditorScreenState
import kotlinx.collections.immutable.toPersistentList
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun EditorScreen(
    state: EditorScreenState,
    selectedType: EditorListType,
    onSelectType: (EditorListType) -> Unit,
    onClickItem: (EditorListItem) -> Unit,
    onRenameItem: (EditorListItem) -> Unit,
    onDeleteItem: (EditorListItem) -> Unit,
    onClickAdd: () -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                navigateUp = navigateUp,
                titleContent = {
                    EditorTypeDropDown(
                        type = selectedType,
                        values = EditorListType.entries.toPersistentList(),
                        onSelect = onSelectType,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionAddButton(
                lazyListState = lazyListState,
                onClick = onClickAdd,
            )
        },
    ) { paddingValues ->
        when (state) {
            EditorScreenState.Loading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
            is EditorScreenState.Success -> {
                if (state.isEmpty) {
                    EmptyScreen(
                        stringRes = AYMR.strings.pref_player_no_items,
                        modifier = Modifier.padding(paddingValues),
                    )
                    return@Scaffold
                }

                EditorListContent(
                    items = state.editorListItems,
                    lazyListState = lazyListState,
                    paddingValues = paddingValues,
                    onClickItem = onClickItem,
                    onRenameItem = onRenameItem,
                    onDeleteItem = onDeleteItem,
                )
            }
        }
    }
}

@Composable
private fun EditorListContent(
    items: List<EditorListItem>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickItem: (EditorListItem) -> Unit,
    onRenameItem: (EditorListItem) -> Unit,
    onDeleteItem: (EditorListItem) -> Unit,
) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        state = lazyListState,
        contentPadding = paddingValues,
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> "editoritem-${item.name}" },
        ) { index, item ->
            FileListItem(
                item = item,
                expanded = index == expandedIndex,
                modifier = Modifier.animateItem(),
                onClick = { onClickItem(item) },
                onExpand = { expanded ->
                    expandedIndex = index.takeIf { expanded }
                },
                onRename = { onRenameItem(item) },
                onDelete = { onDeleteItem(item) },
            )
        }
    }
}

@Composable
private fun FileListItem(
    item: EditorListItem,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onExpand: (Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = modifier
            .clickable { onClick() },
        leadingContent = {
            Icon(
                imageVector = when (item.name.substringAfterLast(".")) {
                    "lua" -> Icons.Outlined.IntegrationInstructions
                    "conf" -> Icons.Outlined.SettingsApplications
                    else -> Icons.Outlined.Description
                },
                null,
            )
        },
        trailingContent = {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpand(false) },
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(AYMR.strings.editor_action_rename)) },
                    onClick = {
                        onRename()
                        onExpand(false)
                    },
                )

                DropdownMenuItem(
                    text = { Text(text = stringResource(AYMR.strings.editor_action_delete)) },
                    onClick = {
                        onDelete()
                        onExpand(false)
                    },
                )
            }

            IconButton(onClick = { onExpand(true) }) {
                Icon(Icons.Filled.MoreHoriz, null)
            }
        },
        headlineContent = {
            Text(text = item.name)
        },
        supportingContent = {
            Text(
                text = listOfNotNull(item.size, item.lastModified).joinToString(" - "),
            )
        },
    )
}
