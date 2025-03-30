package tachiyomi.domain.source.anime.model

import eu.kanade.tachiyomi.animesource.model.FilterList

data class EXHSavedSearch(
    val id: Long,
    val name: String,
    val query: String?,
    val filterList: FilterList?,
)
