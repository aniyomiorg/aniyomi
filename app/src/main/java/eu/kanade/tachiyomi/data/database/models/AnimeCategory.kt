package eu.kanade.tachiyomi.data.database.models

import dataanime.GetCategories

class AnimeCategory {

    var id: Long? = null

    var anime_id: Long = 0

    var category_id: Int = 0

    companion object {

        fun create(anime: Anime, category: Category): AnimeCategory {
            val ac = AnimeCategory()
            ac.anime_id = anime.id!!
            ac.category_id = category.id!!
            return ac
        }

        fun create(anime: Anime, category: GetCategories): AnimeCategory {
            val ac = AnimeCategory()
            ac.anime_id = anime.id!!
            ac.category_id = category.id.toInt()
            return ac
        }
    }
}
