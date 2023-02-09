package eu.kanade.tachiyomi.ui

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animehistory.AnimeHistoryScreenModel
import eu.kanade.tachiyomi.ui.animehistory.animeHistoryTab
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import eu.kanade.tachiyomi.ui.history.historyTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class HistoriesTab() : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(R.string.history),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        // Hoisted for extensions tab's search bar
        val historyScreenModel = rememberScreenModel { HistoryScreenModel() }

        val animeHistoryScreenModel = rememberScreenModel { AnimeHistoryScreenModel() }

        val libraryPreferences: LibraryPreferences = Injekt.get()
        val fromMore = libraryPreferences.bottomNavStyle().get() == 0

        TabbedScreen(
            titleRes = R.string.label_recent_manga,
            tabs = listOf(
                animeHistoryTab(),
                historyTab(),
            ),
            searchQuery = historyScreenModel.getSearchQuery,
            onChangeSearchQuery = historyScreenModel::updateSearchQuery,
            searchQueryAnime = animeHistoryScreenModel.getSearchQuery,
            onChangeSearchQueryAnime = animeHistoryScreenModel::updateSearchQuery,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }

        // For local source
        DiskUtil.RequestStoragePermission()
    }

    fun resumeLastItem() {
        if (state.currentPage == TAB_MANGA) {
            presenter.resumeLastChapterRead()
        } else {
            presenter.resumeLastEpisodeSeen()
        }
    }
}

private const val TAB_ANIME = 0
private const val TAB_MANGA = 1

// TODO: Fix History, updates and download tabs
