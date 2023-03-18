package eu.kanade.data.source.anime

import eu.kanade.domain.source.anime.model.AnimeSource
import eu.kanade.domain.source.anime.model.AnimeSourceData
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager

val animeSourceMapper: (eu.kanade.tachiyomi.animesource.AnimeSource) -> AnimeSource = { source ->
    AnimeSource(
        source.id,
        source.lang,
        source.name,
        supportsLatest = false,
        isStub = source is AnimeSourceManager.StubAnimeSource,
    )
}

val catalogueAnimeSourceMapper: (AnimeCatalogueSource) -> AnimeSource = { source ->
    animeSourceMapper(source).copy(supportsLatest = source.supportsLatest)
}

val animeSourceDataMapper: (Long, String, String) -> AnimeSourceData = { id, lang, name ->
    AnimeSourceData(id, lang, name)
}
