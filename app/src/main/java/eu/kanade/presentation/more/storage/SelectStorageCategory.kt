package eu.kanade.presentation.more.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.SelectItem
import eu.kanade.tachiyomi.R
import tachiyomi.domain.category.model.Category

@Composable
fun SelectStorageCategory(
    selectedCategory: Category,
    categories: List<Category>,
    modifier: Modifier = Modifier,
    onCategorySelected: (Category) -> Unit,
) {
    val all = stringResource(R.string.label_all)
    val default = stringResource(R.string.label_default)
    val mappedCategories = remember(categories) {
        categories.map {
            when (it.id) {
                -1L -> it.copy(name = all)
                Category.UNCATEGORIZED_ID -> it.copy(name = default)
                else -> it
            }
        }.toTypedArray()
    }

    SelectItem(
        modifier = modifier,
        label = stringResource(R.string.label_category),
        selectedIndex = mappedCategories.indexOfFirst { it.id == selectedCategory.id },
        options = mappedCategories,
        onSelect = { index ->
            onCategorySelected(mappedCategories[index])
        },
        toString = { it.name },
    )
}
