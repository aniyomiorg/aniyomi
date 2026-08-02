package mihon.domain.source.models

import tachiyomi.domain.entries.anime.model.Anime

data class RemoteAnimeSeasonUpdate(
    val anime: Anime,
    val newSeasons: List<Anime>,
)
