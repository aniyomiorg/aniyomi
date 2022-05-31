package eu.kanade.domain.animeextension.interactor

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.animeextension.details.AnimeExtensionSourceItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAnimeExtensionSources(
    private val preferences: PreferencesHelper,
) {

    fun subscribe(extension: AnimeExtension.Installed): Flow<List<AnimeExtensionSourceItem>> {
        val isMultiSource = extension.sources.size > 1
        val isMultiLangSingleSource =
            isMultiSource && extension.sources.map { it.name }.distinct().size == 1

        return preferences.disabledSources().asFlow().map { disabledSources ->
            fun AnimeSource.isEnabled() = id.toString() !in disabledSources

            extension.sources
                .map { source ->
                    AnimeExtensionSourceItem(
                        source = source,
                        enabled = source.isEnabled(),
                        labelAsName = isMultiSource && isMultiLangSingleSource.not(),
                    )
                }
        }
    }
}
