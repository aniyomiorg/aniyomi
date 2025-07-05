package eu.kanade.tachiyomi.ui.browse.anime.source

import androidx.compose.runtime.Composable
import eu.kanade.presentation.browse.anime.BrowseTabWrapper
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import java.io.Serializable

/**
 * Navigated to when invoking [AnimeScreen.openSmartSearch] for entries to merge or
 * from [RecommendsScreen.openSmartSearch] for click a recommendation entry.
 * This will show a [sourcesTab] to select a source to search for entries to merge or
 * search for recommending entry.
 */
class SourcesScreen(private val smartSearchConfig: SmartSearchConfig?) : Screen() {
    @Composable
    override fun Content() {
        BrowseTabWrapper(animeSourcesTab(smartSearchConfig))
    }

    /**
     * initialized when invoking [AnimeScreen.openSmartSearch] or [RecommendsScreen.openSmartSearch]
     */
    data class SmartSearchConfig(val origTitle: String, val origMangaId: Long? = null) : Serializable
}
