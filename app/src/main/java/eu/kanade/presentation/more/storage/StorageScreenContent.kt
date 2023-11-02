package eu.kanade.presentation.more.storage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import kotlin.random.Random

@Composable
fun StorageScreenContent(
    state: StorageScreenState,
    isManga: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onCategorySelected: (Category) -> Unit,
    onDelete: (Long) -> Unit,
) {
    when (state) {
        is StorageScreenState.Loading -> {
            LoadingScreen(modifier)
        }

        is StorageScreenState.Success -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = contentPadding + PaddingValues(MaterialTheme.padding.small),
                content = {
                    item {
                        SelectStorageCategory(
                            selectedCategory = state.selectedCategory,
                            categories = state.categories,
                            onCategorySelected = onCategorySelected,
                        )
                    }
                    item {
                        CumulativeStorage(
                            modifier = Modifier
                                .padding(
                                    horizontal = MaterialTheme.padding.small,
                                    vertical = MaterialTheme.padding.medium,
                                )
                                .padding(bottom = MaterialTheme.padding.medium),
                            items = state.items,
                        )
                    }
                    items(
                        state.items.size,
                        itemContent = { index ->
                            StorageItem(
                                item = state.items[index],
                                isManga = isManga,
                                onDelete = onDelete,
                            )
                            Spacer(Modifier.height(MaterialTheme.padding.medium))
                        },
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageScreenContentPreview() {
    val random = remember { Random(0) }
    val categories = remember {
        List(10) {
            Category(
                id = it.toLong(),
                name = "Category $it",
                0L,
                0L,
                false,
            )
        }
    }
    StorageScreenContent(
        state = StorageScreenState.Success(
            items = List(20) { index ->
                StorageItem(
                    id = index.toLong(),
                    title = "Title $index",
                    size = index * 10000000L,
                    thumbnail = null,
                    entriesCount = 100 * index,
                    color = Color(
                        random.nextInt(255),
                        random.nextInt(255),
                        random.nextInt(255),
                    ),
                )
            },
            categories = categories,
            selectedCategory = categories[0],
        ),
        isManga = true,
        contentPadding = PaddingValues(0.dp),
        onCategorySelected = {},
        onDelete = {},
    )
}
