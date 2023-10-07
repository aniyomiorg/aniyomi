package eu.kanade.tachiyomi.ui.category.anime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.AnimeCategoryScreen
import eu.kanade.presentation.category.components.CategoryCreateDialog
import eu.kanade.presentation.category.components.CategoryDeleteDialog
import eu.kanade.presentation.category.components.CategoryRenameDialog
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun Screen.animeCategoryTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { AnimeCategoryScreenModel() }

    val state by screenModel.state.collectAsState()

    return TabContent(
        titleRes = R.string.label_anime,
        searchEnabled = false,
        content = { contentPadding, _ ->
            if (state is AnimeCategoryScreenState.Loading) {
                LoadingScreen()
            } else {
                val successState = state as AnimeCategoryScreenState.Success

                AnimeCategoryScreen(
                    state = successState,
                    contentPadding = contentPadding,
                    onClickCreate = { screenModel.showDialog(AnimeCategoryDialog.Create) },
                    onClickRename = { screenModel.showDialog(AnimeCategoryDialog.Rename(it)) },
                    onClickHide = screenModel::hideCategory,
                    onClickDelete = { screenModel.showDialog(AnimeCategoryDialog.Delete(it)) },
                    onClickMoveUp = screenModel::moveUp,
                    onClickMoveDown = screenModel::moveDown,
                )

                when (val dialog = successState.dialog) {
                    null -> {}
                    AnimeCategoryDialog.Create -> {
                        CategoryCreateDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onCreate = screenModel::createCategory,
                            categories = successState.categories,
                        )
                    }
                    is AnimeCategoryDialog.Rename -> {
                        CategoryRenameDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onRename = { screenModel.renameCategory(dialog.category, it) },
                            categories = successState.categories,
                            category = dialog.category,
                        )
                    }
                    is AnimeCategoryDialog.Delete -> {
                        CategoryDeleteDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onDelete = { screenModel.deleteCategory(dialog.category.id) },
                            category = dialog.category,
                        )
                    }
                }
            }
        },
        navigateUp = navigator::pop,
    )
}
