package eu.kanade.tachiyomi.ui.category.anime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.rememberScreenModel

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.AnimeCategoryScreen
import eu.kanade.presentation.category.components.CategoryCreateDialog
import eu.kanade.presentation.category.components.CategoryDeleteDialog
import eu.kanade.presentation.category.components.CategoryRenameDialog
import eu.kanade.presentation.category.components.ThumbnailUrlDialog
import eu.kanade.presentation.components.TabContent
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun Screen.animeCategoryTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { AnimeCategoryScreenModel() }

    val state by screenModel.state.collectAsState()

    var navigationStack by remember { mutableStateOf(listOf<Long?>(null)) }
    var thumbnailDialogCategory by remember { mutableStateOf<Category?>(null) }
    val currentParentId = navigationStack.last()

    return TabContent(
        titleRes = AYMR.strings.label_anime,
        searchEnabled = false,
        content = { contentPadding, _ ->
            if (state is AnimeCategoryScreenState.Loading) {
                LoadingScreen()
            } else {
                val successState = state as AnimeCategoryScreenState.Success
                val currentCategories = successState.categories.filter { it.parentId == currentParentId }
                val currentState = successState.copy(categories = currentCategories.toImmutableList())
                val allCategories = successState.categories

                Column {
                    if (navigationStack.size > 1) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            IconButton(onClick = { navigationStack = navigationStack.dropLast(1) }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(MR.strings.action_back))
                            }
                            Text(text = stringResource(MR.strings.label_subcategories))
                        }
                    }

                    AnimeCategoryScreen(
                        state = currentState,
                        onClickCategory = { navigationStack = navigationStack + it.id },
                        onClickCreate = { screenModel.showDialog(AnimeCategoryDialog.Create(currentParentId)) },
                        onClickRename = { screenModel.showDialog(AnimeCategoryDialog.Rename(it)) },
                        onClickHide = screenModel::hideCategory,
                        onClickDelete = { screenModel.showDialog(AnimeCategoryDialog.Delete(it)) },
                        onMoveUp = screenModel::moveUp,
                        onMoveDown = screenModel::moveDown,
                        onMoveToParent = screenModel::moveToParent,
                        onEditThumbnail = { thumbnailDialogCategory = it },
                        canMoveUp = { category ->
                            val siblings = allCategories.filter { it.parentId == category.parentId }
                            val sortedSiblings = siblings.sortedWith(compareBy({ it.order }, { it.id }))
                            val index = sortedSiblings.indexOfFirst { it.id == category.id }
                            index > 0
                        },
                        canMoveDown = { category ->
                            val siblings = allCategories.filter { it.parentId == category.parentId }
                            val sortedSiblings = siblings.sortedWith(compareBy({ it.order }, { it.id }))
                            val index = sortedSiblings.indexOfFirst { it.id == category.id }
                            index >= 0 && index < sortedSiblings.size - 1
                        },
                        canMoveToParent = { category ->
                            category.parentId != null
                        },
                    )
                }

                when (val dialog = successState.dialog) {
                    null -> {}
                    is AnimeCategoryDialog.Create -> {
                        CategoryCreateDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onCreate = { screenModel.createCategory(it, dialog.parentId) },
                            categories = successState.categories.fastMap { it.name }.toImmutableList(),
                        )
                    }
                    is AnimeCategoryDialog.Rename -> {
                        CategoryRenameDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onRename = { screenModel.renameCategory(dialog.category, it) },
                            categories = successState.categories.fastMap { it.name }.toImmutableList(),
                            category = dialog.category.name,
                        )
                    }
                    is AnimeCategoryDialog.Delete -> {
                        CategoryDeleteDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onDelete = { screenModel.deleteCategory(dialog.category.id) },
                            category = dialog.category.name,
                        )
                    }
                }

                thumbnailDialogCategory?.let { category ->
                    ThumbnailUrlDialog(
                        currentUrl = category.thumbnailUrl,
                        onDismissRequest = { thumbnailDialogCategory = null },
                        onConfirm = { url -> screenModel.setThumbnail(category.id, url) },
                    )
                }
            }
        },
        navigateUp = navigator::pop,
    )
}
