package tachiyomi.data.source.anime

import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.StubAnimeSource

val animeSourceMapper: (eu.kanade.tachiyomi.animesource.AnimeSource) -> AnimeSource = { source ->
    AnimeSource(
        id = source.id,
        lang = source.lang,
        name = source.name,
        supportsLatest = false,
        isStub = false,
    )
}

val animeSourceDataMapper: (Long, String, String) -> StubAnimeSource = { id, lang, name ->
    StubAnimeSource(id, lang, name)
}
