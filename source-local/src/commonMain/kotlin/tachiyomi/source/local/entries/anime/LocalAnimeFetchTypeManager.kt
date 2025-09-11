package tachiyomi.source.local.entries.anime

import eu.kanade.tachiyomi.animesource.model.FetchType

expect class LocalAnimeFetchTypeManager {
    fun find(animeUrl: String): FetchType
}
