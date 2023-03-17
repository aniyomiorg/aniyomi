package eu.kanade.tachiyomi.data.database.models.anime

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.domain.entries.anime.model.Anime as DomainAnime

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

    private fun setViewerFlags(flag: Int, mask: Int) {
        viewer_flags = viewer_flags and mask.inv() or (flag and mask)
    }

    var skipIntroLength: Int
        get() = viewer_flags and 0x000000FF
        set(skipIntro) = setViewerFlags(skipIntro, 0x000000FF)
}

fun Anime.toDomainAnime(): DomainAnime? {
    val mangaId = id ?: return null
    return DomainAnime(
        id = mangaId,
        source = source,
        favorite = favorite,
        lastUpdate = last_update,
        dateAdded = date_added,
        viewerFlags = viewer_flags.toLong(),
        episodeFlags = episode_flags.toLong(),
        coverLastModified = cover_last_modified,
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = status.toLong(),
        thumbnailUrl = thumbnail_url,
        initialized = initialized,
        updateStrategy = update_strategy,
    )
}
