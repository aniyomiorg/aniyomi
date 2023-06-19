package tachiyomi.data.source.anime

import tachiyomi.domain.source.anime.model.AnimeSourceData

val animeSourceDataMapper: (Long, String, String) -> AnimeSourceData = { id, lang, name ->
    AnimeSourceData(id, lang, name)
}
