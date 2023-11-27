package eu.kanade.tachiyomi.ui.category.manga

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.MangaCategoryScreen
import eu.kanade.presentation.category.components.CategoryCreateDialog
import eu.kanade.presentation.category.components.CategoryDeleteDialog
import eu.kanade.presentation.category.components.CategoryRenameDialog
import eu.kanade.presentation.category.components.CategorySortAlphabeticallyDialog
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun Screen.mangaCategoryTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { MangaCategoryScreenModel() }

    val state by screenModel.state.collectAsState()

    return TabContent(
        titleRes = R.string.label_manga,
        searchEnabled = false,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(R.string.action_sort),
                icon = Icons.Outlined.SortByAlpha,
                onClick = { screenModel.showDialog(MangaCategoryDialog.SortAlphabetically) },
            ),
        ),
        content = { contentPadding, _ ->

            if (state is MangaCategoryScreenState.Loading) {
                LoadingScreen()
            } else {
                val successState = state as MangaCategoryScreenState.Success

                MangaCategoryScreen(
                    state = successState,
                    contentPadding = contentPadding,
                    onClickCreate = { screenModel.showDialog(MangaCategoryDialog.Create) },
                    onClickRename = { screenModel.showDialog(MangaCategoryDialog.Rename(it)) },
                    onClickHide = screenModel::hideCategory,
                    onClickDelete = { screenModel.showDialog(MangaCategoryDialog.Delete(it)) },
                    onClickMoveUp = screenModel::moveUp,
                    onClickMoveDown = screenModel::moveDown,
                )

                when (val dialog = successState.dialog) {
                    null -> {}
                    MangaCategoryDialog.Create -> {
                        CategoryCreateDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onCreate = screenModel::createCategory,
                            categories = successState.categories,
                        )
                    }
                    is MangaCategoryDialog.Rename -> {
                        CategoryRenameDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onRename = { screenModel.renameCategory(dialog.category, it) },
                            categories = successState.categories,
                            category = dialog.category,
                        )
                    }
                    is MangaCategoryDialog.Delete -> {
                        CategoryDeleteDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onDelete = { screenModel.deleteCategory(dialog.category.id) },
                            category = dialog.category,
                        )
                    }
                    is MangaCategoryDialog.SortAlphabetically -> {
                        CategorySortAlphabeticallyDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onSort = { screenModel.sortAlphabetically() },
                        )
                    }
                }
            }
        },
        navigateUp = navigator::pop,
    )
}
