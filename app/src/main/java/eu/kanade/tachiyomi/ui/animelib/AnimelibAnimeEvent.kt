package eu.kanade.tachiyomi.ui.animelib

import eu.kanade.domain.category.model.Category

class AnimelibAnimeEvent(val animes: AnimelibMap) {

    fun getAnimeForCategory(category: Category): List<AnimelibItem>? {
        return animes[category.id]
    }
}
