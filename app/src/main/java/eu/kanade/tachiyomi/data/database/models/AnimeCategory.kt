package eu.kanade.tachiyomi.data.database.models

class AnimeCategory {

    var id: Long? = null

    var anime_id: Long = 0

    var category_id: Int = 0

    companion object {

        fun create(anime: Anime, category: Category): AnimeCategory {
            val mc = AnimeCategory()
            mc.anime_id = anime.id!!
            mc.category_id = category.id!!
            return mc
        }
    }
}
