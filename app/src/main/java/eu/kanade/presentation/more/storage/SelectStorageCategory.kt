package eu.kanade.presentation.more.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SelectStorageCategory(
    selectedCategory: Category,
    categories: List<Category>,
    modifier: Modifier = Modifier,
    onCategorySelected: (Category) -> Unit,
) {
    val all = stringResource(AYMR.strings.label_all)
    val default = stringResource(MR.strings.label_default)
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
        label = stringResource(AYMR.strings.label_category),
        selectedIndex = mappedCategories.indexOfFirst { it.id == selectedCategory.id },
        options = mappedCategories,
        onSelect = { index ->
            onCategorySelected(mappedCategories[index])
        },
        toString = { it.name },
    )
}
