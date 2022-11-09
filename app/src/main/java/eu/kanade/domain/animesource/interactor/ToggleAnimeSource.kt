package eu.kanade.domain.animesource.interactor

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.preference.getAndSet

class ToggleAnimeSource(
    private val preferences: SourcePreferences,
) {

    fun await(source: AnimeSource, enable: Boolean = isEnabled(source.id)) {
        await(source.id, enable)
    }

    fun await(sourceId: Long, enable: Boolean = isEnabled(sourceId)) {
        preferences.disabledAnimeSources().getAndSet { disabled ->
            if (enable) disabled.minus("$sourceId") else disabled.plus("$sourceId")
        }
    }

    fun await(sourceIds: List<Long>, enable: Boolean) {
        val transformedSourceIds = sourceIds.map { it.toString() }
        preferences.disabledAnimeSources().getAndSet { disabled ->
            if (enable) disabled.minus(transformedSourceIds) else disabled.plus(transformedSourceIds)
        }
    }

    private fun isEnabled(sourceId: Long): Boolean {
        return sourceId.toString() in preferences.disabledAnimeSources().get()
    }
}
