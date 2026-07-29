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

    private enum class TabSelectors {
        TAB_MANGA_EXTENSION,
        TAB_ANIME_EXTENSION,
    }

    private val switchToTabChannel = Channel<TabSelectors>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToTabChannel.trySend(TabSelectors.TAB_MANGA_EXTENSION)
    }

    fun showAnimeExtension() {
        switchToTabChannel.trySend(TabSelectors.TAB_ANIME_EXTENSION)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        // Hoisted for extensions tab's search bar
        val mangaExtensionsScreenModel = rememberScreenModel { MangaExtensionsScreenModel() }
        val mangaExtensionsState by mangaExtensionsScreenModel.state.collectAsState()

        val animeExtensionsScreenModel = rememberScreenModel { AnimeExtensionsScreenModel() }
        val animeExtensionsState by animeExtensionsScreenModel.state.collectAsState()

        val (tabs, animeExtensionsIndex: Int, mangaExtensionsIndex: Int?) = run {
            val hideManga = Injekt.get<UiPreferences>().hideManga().get()

            var animeExtensionsIndex: Int
            var mangaExtensionsIndex: Int? = null
            val tabsListBuilder = persistentListOf<TabContent>().builder()

            tabsListBuilder.add(animeSourcesTab())

            if (!hideManga) {
                tabsListBuilder.add(mangaSourcesTab())
            }

            tabsListBuilder.add(animeExtensionsTab(animeExtensionsScreenModel))
            animeExtensionsIndex = tabsListBuilder.size - 1

            if (!hideManga) {
                tabsListBuilder.add(mangaExtensionsTab(mangaExtensionsScreenModel))
                mangaExtensionsIndex = tabsListBuilder.size - 1
            }

            tabsListBuilder.add(migrateAnimeSourceTab())

            if (!hideManga) {
                tabsListBuilder.add(migrateMangaSourceTab())
            }

            Triple(tabsListBuilder.build(), animeExtensionsIndex, mangaExtensionsIndex)
        }

        val state = rememberPagerState { tabs.size }

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
                .collectLatest {
                    val tabToScrollTo: Int = when (it) {
                        TabSelectors.TAB_ANIME_EXTENSION -> animeExtensionsIndex
                        // If we try to show the manga extensions tab when it is hidden, show anime extensions instead
                        TabSelectors.TAB_MANGA_EXTENSION -> mangaExtensionsIndex ?: animeExtensionsIndex
                    }
                    state.scrollToPage(tabToScrollTo)
                }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
