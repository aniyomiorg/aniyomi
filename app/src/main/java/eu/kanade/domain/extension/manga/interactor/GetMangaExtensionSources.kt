package eu.kanade.domain.extension.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetExtensionSources(
    private val preferences: SourcePreferences,
) {

    fun subscribe(extension: Extension.Installed): Flow<List<MangaExtensionSourceItem>> {
        val isMultiSource = extension.sources.size > 1
        val isMultiLangSingleSource =
            isMultiSource && extension.sources.map { it.name }.distinct().size == 1

        return preferences.disabledMangaSources().changes().map { disabledSources ->
            fun Source.isEnabled() = id.toString() !in disabledSources

            extension.sources
                .map { source ->
                    MangaExtensionSourceItem(
                        source = source,
                        enabled = source.isEnabled(),
                        labelAsName = isMultiSource && isMultiLangSingleSource.not(),
                    )
                }
        }
    }
}

data class MangaExtensionSourceItem(
    val source: Source,
    val enabled: Boolean,
    val labelAsName: Boolean,
)
