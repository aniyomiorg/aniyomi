package eu.kanade.tachiyomi.ui.browse.anime.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSearchItemResult
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.AnimeSearchScreenModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateAnimeSearchScreenModel(
    val animeId: Long,
    initialExtensionFilter: String = "",
    preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
) : AnimeSearchScreenModel<MigrateAnimeSearchState>(MigrateAnimeSearchState()) {

    init {
        extensionFilter = initialExtensionFilter
        coroutineScope.launch {
            val anime = getAnime.await(animeId)!!

            mutableState.update {
                it.copy(anime = anime, searchQuery = anime.title)
            }

            search(anime.title)
        }
    }

    val incognitoMode = preferences.incognitoMode()
    val lastUsedSourceId = sourcePreferences.lastUsedAnimeSource()

    override fun getEnabledSources(): List<AnimeCatalogueSource> {
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledAnimeSources().get()
        val pinnedSources = sourcePreferences.pinnedAnimeSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in enabledLanguages }
            .filterNot { "${it.id}" in disabledSources }
            .sortedWith(compareBy({ "${it.id}" !in pinnedSources }, { "${it.name.lowercase()} (${it.lang})" }))
            .sortedByDescending { it.id == state.value.anime!!.source }
    }

    override fun updateSearchQuery(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    override fun updateItems(items: Map<AnimeCatalogueSource, AnimeSearchItemResult>) {
        mutableState.update {
            it.copy(items = items)
        }
    }

    override fun getItems(): Map<AnimeCatalogueSource, AnimeSearchItemResult> {
        return mutableState.value.items
    }

    fun setDialog(dialog: MigrateAnimeSearchDialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }
}

sealed class MigrateAnimeSearchDialog {
    data class Migrate(val anime: Anime) : MigrateAnimeSearchDialog()
}

@Immutable
data class MigrateAnimeSearchState(
    val anime: Anime? = null,
    val searchQuery: String? = null,
    val items: Map<AnimeCatalogueSource, AnimeSearchItemResult> = emptyMap(),
    val dialog: MigrateAnimeSearchDialog? = null,
) {

    val progress: Int = items.count { it.value !is AnimeSearchItemResult.Loading }

    val total: Int = items.size
}
