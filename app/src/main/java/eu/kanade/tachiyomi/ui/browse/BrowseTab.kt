package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.extension.animeExtensionsTab
import eu.kanade.tachiyomi.ui.browse.anime.migration.sources.migrateAnimeSourceTab
import eu.kanade.tachiyomi.ui.browse.anime.source.animeSourcesTab
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.manga.extension.mangaExtensionsTab
import eu.kanade.tachiyomi.ui.browse.manga.migration.sources.migrateMangaSourceTab
import eu.kanade.tachiyomi.ui.browse.manga.source.mangaSourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current is BrowseTab
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    // TODO: Find a way to let it open Global Anime/Manga Search depending on what Tab(e.g. Anime/Manga Source Tab) is open
    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalAnimeSearchScreen())
    }

    private val switchToTabChannel = Channel<BrowseTabSubtab>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToTabChannel.trySend(BrowseTabSubtab.TAB_MANGA_EXTENSIONS)
    }

    fun showAnimeExtension() {
        switchToTabChannel.trySend(BrowseTabSubtab.TAB_ANIME_EXTENSIONS)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        // Hoisted for extensions tab's search bar
        val mangaExtensionsScreenModel = rememberScreenModel { MangaExtensionsScreenModel() }
        val mangaExtensionsState by mangaExtensionsScreenModel.state.collectAsState()

        val animeExtensionsScreenModel = rememberScreenModel { AnimeExtensionsScreenModel() }
        val animeExtensionsState by animeExtensionsScreenModel.state.collectAsState()

        val hideManga by Injekt.get<UiPreferences>().hideManga().collectAsState()

        val tabs = persistentListOf<TabContent>().builder().apply {
            add(animeSourcesTab())
            if (!hideManga) add(mangaSourcesTab())
            add(animeExtensionsTab(animeExtensionsScreenModel))
            if (!hideManga) add(mangaExtensionsTab(mangaExtensionsScreenModel))
            add(migrateAnimeSourceTab())
            if (!hideManga) add(migrateMangaSourceTab())
        }.build()

        val state = rememberPagerState { tabs.size }

        LaunchedEffect(tabs.size) {
            if ((state.settledPage >= tabs.size || state.currentPage >= tabs.size) && !tabs.isEmpty()) {
                state.scrollToPage(tabs.size - 1)
            }
        }
        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs,
            state = state,
            mangaSearchQuery = mangaExtensionsState.searchQuery,
            onChangeMangaSearchQuery = mangaExtensionsScreenModel::search,
            animeSearchQuery = animeExtensionsState.searchQuery,
            onChangeAnimeSearchQuery = animeExtensionsScreenModel::search,
            scrollable = true,
        )
        LaunchedEffect(Unit) {
            switchToTabChannel.receiveAsFlow()
                .collectLatest { tabKeyToScrollTo ->
                    var tabIndexToScrollTo: Int = tabs.indexOfFirst { it.tabKey == tabKeyToScrollTo }
                    if (tabIndexToScrollTo < 0) {
                        // If we didn't find the tab we were looking for, check for the corresponding anime/manga tab
                        val fallbackTabKey: BrowseTabSubtab = when (tabKeyToScrollTo) {
                            BrowseTabSubtab.TAB_MANGA_SOURCES -> BrowseTabSubtab.TAB_ANIME_SOURCES
                            BrowseTabSubtab.TAB_MANGA_EXTENSIONS -> BrowseTabSubtab.TAB_ANIME_EXTENSIONS
                            BrowseTabSubtab.TAB_MANGA_MIGRATE -> BrowseTabSubtab.TAB_ANIME_MIGRATE
                            BrowseTabSubtab.TAB_ANIME_SOURCES -> BrowseTabSubtab.TAB_MANGA_SOURCES
                            BrowseTabSubtab.TAB_ANIME_EXTENSIONS -> BrowseTabSubtab.TAB_MANGA_EXTENSIONS
                            BrowseTabSubtab.TAB_ANIME_MIGRATE -> BrowseTabSubtab.TAB_MANGA_MIGRATE
                        }
                        tabIndexToScrollTo = tabs.indexOfFirst { it.tabKey == fallbackTabKey }
                    }

                    if (tabIndexToScrollTo >= 0) {
                        state.scrollToPage(tabIndexToScrollTo)
                    }
                }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
