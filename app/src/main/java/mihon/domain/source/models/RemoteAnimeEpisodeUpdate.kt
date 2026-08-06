package mihon.domain.source.models

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode

data class RemoteAnimeEpisodeUpdate(
    val anime: Anime,
    val newEpisodes: List<Episode>,
)
