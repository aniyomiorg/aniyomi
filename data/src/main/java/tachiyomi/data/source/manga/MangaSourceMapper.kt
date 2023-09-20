package tachiyomi.data.source.manga

import tachiyomi.domain.source.manga.model.MangaSourceData
import tachiyomi.domain.source.manga.model.Source

val mangaSourceMapper: (eu.kanade.tachiyomi.source.MangaSource) -> Source = { source ->
    Source(
        source.id,
        source.lang,
        source.name,
        supportsLatest = false,
        isStub = false,
    )
}

val mangaSourceDataMapper: (Long, String, String) -> MangaSourceData = { id, lang, name ->
    MangaSourceData(id, lang, name)
}
