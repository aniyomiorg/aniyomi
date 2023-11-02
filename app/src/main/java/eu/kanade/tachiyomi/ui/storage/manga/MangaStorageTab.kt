package eu.kanade.tachiyomi.ui.storage.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.storage.StorageScreenContent
import eu.kanade.tachiyomi.R

@Composable
fun Screen.mangaStorageTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow

    val screenModel = rememberScreenModel { MangaStorageScreenModel() }
    val state by screenModel.state.collectAsState()

    return TabContent(
        titleRes = R.string.label_manga,
        content = { contentPadding, _ ->
            StorageScreenContent(
                state = state,
                isManga = true,
                contentPadding = contentPadding,
                onCategorySelected = screenModel::setSelectedCategory,
                onDelete = screenModel::deleteEntry,
            )
        },
        navigateUp = navigator::pop,
    )
}
