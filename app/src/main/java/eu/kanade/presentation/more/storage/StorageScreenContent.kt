package eu.kanade.presentation.more.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.isTabletUi
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.screens.LoadingScreen
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
            @Composable
            fun Info(modifier: Modifier = Modifier) {
                Column(
                    modifier = modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    content = {
                        SelectStorageCategory(
                            selectedCategory = state.selectedCategory,
                            categories = state.categories,
                            onCategorySelected = onCategorySelected,
                        )
                        CumulativeStorage(
                            modifier = Modifier
                                .padding(
                                    horizontal = MaterialTheme.padding.small,
                                    vertical = MaterialTheme.padding.medium,
                                )
                                .run {
                                    if (isTabletUi()) {
                                        this
                                    } else {
                                        padding(bottom = MaterialTheme.padding.medium)
                                    }
                                },
                            items = state.items,
                        )
                    },
                )
            }

            Row(
                modifier = modifier
                    .padding(horizontal = MaterialTheme.padding.small)
                    .padding(contentPadding),
                content = {
                    if (isTabletUi()) {
                        Info(
                            modifier = Modifier
                                .weight(2f)
                                .padding(end = MaterialTheme.padding.extraLarge)
                                .fillMaxHeight(),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(3f),
                        content = {
                            item {
                                Spacer(Modifier.height(MaterialTheme.padding.small))
                            }
                            item {
                                if (!isTabletUi()) {
                                    Info()
                                }
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

@Preview(showBackground = true, device = Devices.DESKTOP)
@Composable
private fun StorageTabletUiScreenContentPreview() {
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
