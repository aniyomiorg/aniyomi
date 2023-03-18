package eu.kanade.data.source.manga

import eu.kanade.domain.source.manga.model.MangaSourceData
import eu.kanade.domain.source.manga.model.Source
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.manga.MangaSourceManager

val mangaSourceMapper: (eu.kanade.tachiyomi.source.MangaSource) -> Source = { source ->
    Source(
        source.id,
        source.lang,
        source.name,
        supportsLatest = false,
        isStub = source is MangaSourceManager.StubMangaSource,
    )
}

val catalogueMangaSourceMapper: (CatalogueSource) -> Source = { source ->
    mangaSourceMapper(source).copy(supportsLatest = source.supportsLatest)
}

val mangaSourceDataMapper: (Long, String, String) -> MangaSourceData = { id, lang, name ->
    MangaSourceData(id, lang, name)
}
