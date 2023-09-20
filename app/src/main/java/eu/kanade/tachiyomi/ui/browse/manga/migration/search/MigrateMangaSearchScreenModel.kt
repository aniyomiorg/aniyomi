package eu.kanade.tachiyomi.ui.browse.manga.migration.search

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSearchItemResult
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSearchScreenModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateSearchScreenModel(
    val mangaId: Long,
    initialExtensionFilter: String = "",
    preferences: BasePreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: MangaSourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : MangaSearchScreenModel<MigrateMangaSearchState>(MigrateMangaSearchState()) {

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

    override fun getEnabledSources(): List<CatalogueSource> {
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledMangaSources().get()
        val pinnedSources = sourcePreferences.pinnedMangaSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in enabledLanguages }
            .filterNot { "${it.id}" in disabledSources }
            .sortedWith(compareBy({ "${it.id}" !in pinnedSources }, { "${it.name.lowercase()} (${it.lang})" }))
            .sortedByDescending { it.id == state.value.manga!!.source }
    }

    override fun updateSearchQuery(query: String?) {
        mutableState.update {
            it.copy(searchQuery = query)
        }
    }

    override fun updateItems(items: Map<CatalogueSource, MangaSearchItemResult>) {
        mutableState.update {
            it.copy(items = items)
        }
    }

    override fun getItems(): Map<CatalogueSource, MangaSearchItemResult> {
        return mutableState.value.items
    }

    fun setDialog(dialog: MigrateMangaSearchDialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }
}

sealed class MigrateMangaSearchDialog {
    data class Migrate(val manga: Manga) : MigrateMangaSearchDialog()
}

@Immutable
data class MigrateMangaSearchState(
    val manga: Manga? = null,
    val searchQuery: String? = null,
    val items: Map<CatalogueSource, MangaSearchItemResult> = emptyMap(),
    val dialog: MigrateMangaSearchDialog? = null,
) {

    val progress: Int = items.count { it.value !is MangaSearchItemResult.Loading }

    val total: Int = items.size
}
