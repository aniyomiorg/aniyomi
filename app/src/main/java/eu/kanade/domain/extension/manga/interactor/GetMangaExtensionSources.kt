package eu.kanade.domain.extension.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.source.MangaSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetExtensionSources(
    private val preferences: SourcePreferences,
) {

    fun subscribe(extension: MangaExtension.Installed): Flow<List<MangaExtensionSourceItem>> {
        val isMultiSource = extension.sources.size > 1
        val isMultiLangSingleSource =
            isMultiSource && extension.sources.map { it.name }.distinct().size == 1

        return preferences.disabledMangaSources().changes().map { disabledSources ->
            fun MangaSource.isEnabled() = id.toString() !in disabledSources

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
    val source: MangaSource,
    val enabled: Boolean,
    val labelAsName: Boolean,
)
