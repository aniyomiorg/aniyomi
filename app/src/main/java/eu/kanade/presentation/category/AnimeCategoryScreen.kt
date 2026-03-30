package eu.kanade.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.category.components.CategoryListItem
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryScreenState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun AnimeCategoryScreen(
    state: AnimeCategoryScreenState.Success,
    onClickCategory: (Category) -> Unit,
    onClickCreate: () -> Unit,
    onClickRename: (Category) -> Unit,
    onClickHide: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onMoveUp: (Category) -> Unit,
    onMoveDown: (Category) -> Unit,
    onMoveToParent: (Category) -> Unit,
    onEditThumbnail: (Category) -> Unit,
    canMoveUp: (Category) -> Boolean,
    canMoveDown: (Category) -> Boolean,
    canMoveToParent: (Category) -> Boolean,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                stringRes = MR.strings.information_empty_category,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.medium,
                top = MaterialTheme.padding.medium + paddingValues.calculateTopPadding(),
                bottom = MaterialTheme.padding.medium + paddingValues.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            items(
                items = state.categories,
                key = { category -> "category-${category.id}" },
            ) { category ->
                CategoryListItem(
                    category = category,
                    onClick = { onClickCategory(category) },
                    onRename = { onClickRename(category) },
                    onHide = { onClickHide(category) },
                    onDelete = { onClickDelete(category) },
                    onMoveUp = if (canMoveUp(category)) {
                        { onMoveUp(category) }
                    } else {
                        null
                    },
                    onMoveDown = if (canMoveDown(category)) {
                        { onMoveDown(category) }
                    } else {
                        null
                    },
                    onMoveToParent = if (canMoveToParent(category)) {
                        { onMoveToParent(category) }
                    } else {
                        null
                    },
                    onEditThumbnail = { onEditThumbnail(category) },
                )
            }
        }
    }
}
