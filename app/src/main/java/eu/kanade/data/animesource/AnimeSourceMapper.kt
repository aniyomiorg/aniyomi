package eu.kanade.data.animesource

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.AnimeSourceData
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager

val animesourceMapper: (eu.kanade.tachiyomi.animesource.AnimeSource) -> AnimeSource = { source ->
    AnimeSource(
        source.id,
        source.lang,
        source.name,
        supportsLatest = false,
        isStub = source is AnimeSourceManager.StubAnimeSource,
    )
}

val catalogueSourceMapper: (AnimeCatalogueSource) -> AnimeSource = { source ->
    animesourceMapper(source).copy(supportsLatest = source.supportsLatest)
}

val animesourceDataMapper: (Long, String, String) -> AnimeSourceData = { id, lang, name ->
    AnimeSourceData(id, lang, name)
}
