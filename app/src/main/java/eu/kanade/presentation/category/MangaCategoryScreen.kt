package eu.kanade.presentation.category

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryContent
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.category.manga.MangaCategoryScreenState
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun MangaCategoryScreen(
    state: MangaCategoryScreenState.Success,
    contentPadding: PaddingValues,
    onClickCreate: () -> Unit,
    onClickRename: (Category) -> Unit,
    onClickHide: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onClickMoveUp: (Category) -> Unit,
    onClickMoveDown: (Category) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) {
        if (state.isEmpty) {
            EmptyScreen(
                textResource = R.string.information_empty_category,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }

        CategoryContent(
            categories = state.categories,
            lazyListState = lazyListState,
            paddingValues = contentPadding + topSmallPaddingValues + PaddingValues(horizontal = MaterialTheme.padding.medium),
            onClickRename = onClickRename,
            onClickHide = onClickHide,
            onClickDelete = onClickDelete,
            onMoveUp = onClickMoveUp,
            onMoveDown = onClickMoveDown,
        )
    }
}
