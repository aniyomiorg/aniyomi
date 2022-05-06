package eu.kanade.data.animesource

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource

val animesourceMapper: (eu.kanade.tachiyomi.animesource.AnimeSource) -> AnimeSource = { source ->
    AnimeSource(
        source.id,
        source.lang,
        source.name,
        false
    )
}

val catalogueSourceMapper: (AnimeCatalogueSource) -> AnimeSource = { source ->
    animesourceMapper(source).copy(supportsLatest = source.supportsLatest)
}
