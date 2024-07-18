package eu.kanade.tachiyomi.animesource.model

import androidx.compose.runtime.Stable

@Stable
data class AnimeFilterList(val list: List<AnimeFilter<*>>) : List<AnimeFilter<*>> by list {

    constructor(vararg fs: AnimeFilter<*>) : this(if (fs.isNotEmpty()) fs.asList() else emptyList())

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }
}
