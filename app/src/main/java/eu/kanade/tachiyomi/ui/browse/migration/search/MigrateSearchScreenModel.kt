package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.entries.manga.interactor.GetManga
import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueMangaSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSearchScreenModel(
    val mangaId: Long,
    initialExtensionFilter: String = "",
    preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : SearchScreenModel<MigrateMangaSearchState>(MigrateMangaSearchState()) {

    init {
        extensionFilter = initialExtensionFilter
        coroutineScope.launch {
            val manga = getManga.await(mangaId)!!

            mutableState.update {
                it.copy(manga = manga, searchQuery = manga.title)
            }

            search(manga.title)
        }
    }

    val incognitoMode = preferences.incognitoMode()
    val lastUsedSourceId = sourcePreferences.lastUsedMangaSource()

    override fun getEnabledSources(): List<CatalogueMangaSource> {
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledMangaSources().get()
        val pinnedSources = sourcePreferences.pinnedMangaSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in enabledLanguages }
            .filterNot { "${it.id}" in disabledSources }
            .sortedWith(compareBy({ "${it.id}" !in pinnedSources }, { "${it.name.lowercase()} (${it.lang})" }))
            .sortedByDescending { it.id == state.value.manga!!.id }
    }

    override fun updateSearchQuery(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    override fun updateItems(items: Map<CatalogueMangaSource, SearchItemResult>) {
        mutableState.update {
            it.copy(items = items)
        }
    }

    override fun getItems(): Map<CatalogueMangaSource, SearchItemResult> {
        return mutableState.value.items
    }

    fun setDialog(dialog: MigrateSearchDialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }
}

sealed class MigrateSearchDialog {
    data class Migrate(val manga: Manga) : MigrateSearchDialog()
}

@Immutable
data class MigrateMangaSearchState(
    val manga: Manga? = null,
    val searchQuery: String? = null,
    val items: Map<CatalogueMangaSource, SearchItemResult> = emptyMap(),
    val dialog: MigrateSearchDialog? = null,
) {

    val progress: Int = items.count { it.value !is SearchItemResult.Loading }

    val total: Int = items.size
}
