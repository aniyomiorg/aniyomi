package tachiyomi.data.source.anime

import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.StubAnimeSource

val animeSourceMapper: (eu.kanade.tachiyomi.animesource.AnimeSource) -> AnimeSource = { source ->
    AnimeSource(
        source.id,
        source.lang,
        source.name,
        supportsLatest = false,
        isStub = false,
    )
}

val animeSourceDataMapper: (Long, String, String) -> StubAnimeSource = { id, lang, name ->
    StubAnimeSource(id, lang, name)
}
