package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.collections.immutable.ImmutableList
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

private val uiPreferences: UiPreferences = Injekt.get()

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

    private enum class TabId(val fallbackTab: TabId?) {
        TAB_ANIME_SOURCES(null),
        TAB_MANGA_SOURCES(TAB_ANIME_SOURCES),
        TAB_ANIME_EXTENSIONS(null),
        TAB_MANGA_EXTENSIONS(TAB_ANIME_EXTENSIONS),
        TAB_ANIME_MIGRATE(null),
        TAB_MANGA_MIGRATE(TAB_ANIME_MIGRATE),
    }

    private val switchToTabChannel = Channel<TabId>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToTabChannel.trySend(TabId.TAB_MANGA_EXTENSIONS)
    }

    fun showAnimeExtension() {
        switchToTabChannel.trySend(TabId.TAB_ANIME_EXTENSIONS)
    }

    private data class TabContentWithId(val tabContent: TabContent, val tabId: TabId)

    private fun indexOfTabWithIdOrNull(list: List<TabContentWithId>, id: TabId?): Int? {
        return id?.let {
            list.indexOfFirst { it.tabId == id }.takeIf { it >= 0 }
        }
    }

    // Wraps an ImmutableList<TabContentWithId> to act like a ImmutableList<TabContent>
    private class TabListAdapter(
        val list: ImmutableList<TabContentWithId>,
    ) : AbstractList<TabContent>(), ImmutableList<TabContent> {
        override val size: Int get() = list.size
        override fun get(index: Int): TabContent = list[index].tabContent
        override fun subList(
            fromIndex: Int,
            toIndex: Int,
        ): ImmutableList<TabContent> = super<ImmutableList>.subList(fromIndex, toIndex)
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
        val tabs = run {
            val tabsListBuilder = persistentListOf<TabContentWithId>().builder()

            tabsListBuilder.add(TabContentWithId(animeSourcesTab(), TabId.TAB_ANIME_SOURCES))
            if (!hideManga) tabsListBuilder.add(TabContentWithId(mangaSourcesTab(), TabId.TAB_MANGA_SOURCES))
            tabsListBuilder.add(
                TabContentWithId(animeExtensionsTab(animeExtensionsScreenModel), TabId.TAB_ANIME_EXTENSIONS),
            )
            if (!hideManga) {
                tabsListBuilder.add(
                    TabContentWithId(mangaExtensionsTab(mangaExtensionsScreenModel), TabId.TAB_MANGA_EXTENSIONS),
                )
            }
            tabsListBuilder.add(TabContentWithId(migrateAnimeSourceTab(), TabId.TAB_ANIME_MIGRATE))
            if (!hideManga) tabsListBuilder.add(TabContentWithId(migrateMangaSourceTab(), TabId.TAB_MANGA_MIGRATE))

            tabsListBuilder.build()
        }

        val state = rememberPagerState { tabs.size }
        var currentTabId by remember { mutableStateOf<TabId?>(tabs.getOrNull(state.settledPage)?.tabId) }

        LaunchedEffect(state.settledPage) {
            val tabId: TabId? = tabs.getOrNull(state.settledPage)?.tabId
            if (tabId !== null) {
                currentTabId = tabId
            }
        }
        LaunchedEffect(tabs) {
            // Whenever the list of tabs changes, scroll to the correct page (or its fallback) if possible. If not,
            // just ensure that we are within the bounds of list of tabs
            val tabIndex: Int? =
                indexOfTabWithIdOrNull(tabs, currentTabId) ?: indexOfTabWithIdOrNull(tabs, currentTabId?.fallbackTab)
            if (tabIndex !== null && tabIndex < tabs.size) {
                state.scrollToPage(tabIndex)
            } else if (state.currentPage >= tabs.size) {
                state.scrollToPage(tabs.size - 1)
            }
        }
        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = TabListAdapter(tabs),
            state = state,
            mangaSearchQuery = mangaExtensionsState.searchQuery,
            onChangeMangaSearchQuery = mangaExtensionsScreenModel::search,
            animeSearchQuery = animeExtensionsState.searchQuery,
            onChangeAnimeSearchQuery = animeExtensionsScreenModel::search,
            scrollable = true,
        )
        LaunchedEffect(Unit) {
            switchToTabChannel.receiveAsFlow()
                .collectLatest {
                    val tabToScrollTo: Int? =
                        indexOfTabWithIdOrNull(tabs, it) ?: indexOfTabWithIdOrNull(tabs, it.fallbackTab)
                    if (tabToScrollTo !== null) {
                        state.scrollToPage(tabToScrollTo)
                    }
                }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
