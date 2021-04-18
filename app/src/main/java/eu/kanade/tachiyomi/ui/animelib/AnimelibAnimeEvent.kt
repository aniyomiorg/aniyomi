package eu.kanade.tachiyomi.ui.animelib

import eu.kanade.tachiyomi.data.database.models.Category

class AnimelibAnimeEvent(val animes: Map<Int, List<AnimelibItem>>) {

    fun getAnimeForCategory(category: Category): List<AnimelibItem>? {
        return animes[category.id]
    }
}
