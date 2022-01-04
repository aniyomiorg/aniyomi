package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.animesource.model.SAnime
import tachiyomi.animesource.model.AnimeInfo

interface Anime : SAnime {

    var id: Long?

    var source: Long

    var favorite: Boolean

    // last time the episode list changed in any way
    var last_update: Long

    var date_added: Long

    var viewer_flags: Int

    var episode_flags: Int

    var cover_last_modified: Long

    fun setEpisodeOrder(order: Int) {
        setEpisodeFlags(order, EPISODE_SORT_MASK)
    }

    fun sortDescending(): Boolean {
        return episode_flags and EPISODE_SORT_MASK == EPISODE_SORT_DESC
    }

    fun getGenres(): List<String>? {
        if (genre.isNullOrBlank()) return null
        return genre?.split(", ")?.map { it.trim() }?.filterNot { it.isBlank() }?.distinct()
    }

    private fun setEpisodeFlags(flag: Int, mask: Int) {
        episode_flags = episode_flags and mask.inv() or (flag and mask)
    }

    private fun setViewerFlags(flag: Int, mask: Int) {
        viewer_flags = viewer_flags and mask.inv() or (flag and mask)
    }

    // Used to display the episode's title one way or another
    var displayMode: Int
        get() = episode_flags and EPISODE_DISPLAY_MASK
        set(mode) = setEpisodeFlags(mode, EPISODE_DISPLAY_MASK)

    var seenFilter: Int
        get() = episode_flags and EPISODE_SEEN_MASK
        set(filter) = setEpisodeFlags(filter, EPISODE_SEEN_MASK)

    var downloadedFilter: Int
        get() = episode_flags and EPISODE_DOWNLOADED_MASK
        set(filter) = setEpisodeFlags(filter, EPISODE_DOWNLOADED_MASK)

    var bookmarkedFilter: Int
        get() = episode_flags and EPISODE_BOOKMARKED_MASK
        set(filter) = setEpisodeFlags(filter, EPISODE_BOOKMARKED_MASK)

    var sorting: Int
        get() = episode_flags and EPISODE_SORTING_MASK
        set(sort) = setEpisodeFlags(sort, EPISODE_SORTING_MASK)

    companion object {

        const val EPISODE_SORT_DESC = 0x00000000
        const val EPISODE_SORT_ASC = 0x00000001
        const val EPISODE_SORT_MASK = 0x00000001

        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000

        const val EPISODE_SHOW_UNSEEN = 0x00000002
        const val EPISODE_SHOW_SEEN = 0x00000004
        const val EPISODE_SEEN_MASK = 0x00000006

        const val EPISODE_SHOW_DOWNLOADED = 0x00000008
        const val EPISODE_SHOW_NOT_DOWNLOADED = 0x00000010
        const val EPISODE_DOWNLOADED_MASK = 0x00000018

        const val EPISODE_SHOW_BOOKMARKED = 0x00000020
        const val EPISODE_SHOW_NOT_BOOKMARKED = 0x00000040
        const val EPISODE_BOOKMARKED_MASK = 0x00000060

        const val EPISODE_SORTING_SOURCE = 0x00000000
        const val EPISODE_SORTING_NUMBER = 0x00000100
        const val EPISODE_SORTING_UPLOAD_DATE = 0x00000200
        const val EPISODE_SORTING_MASK = 0x00000300

        const val EPISODE_DISPLAY_NAME = 0x00000000
        const val EPISODE_DISPLAY_NUMBER = 0x00100000
        const val EPISODE_DISPLAY_MASK = 0x00100000

        fun create(source: Long): Anime = AnimeImpl().apply {
            this.source = source
        }

        fun create(pathUrl: String, title: String, source: Long = 0): Anime = AnimeImpl().apply {
            url = pathUrl
            this.title = title
            this.source = source
        }
    }
}

fun Anime.toAnimeInfo(): AnimeInfo {
    return AnimeInfo(
        artist = this.artist ?: "",
        author = this.author ?: "",
        cover = this.thumbnail_url ?: "",
        description = this.description ?: "",
        genres = this.getGenres() ?: emptyList(),
        key = this.url,
        status = this.status,
        title = this.title
    )
}
