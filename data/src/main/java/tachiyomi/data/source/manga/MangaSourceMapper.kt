package tachiyomi.data.source.manga

import tachiyomi.domain.source.manga.model.MangaSourceData

val mangaSourceDataMapper: (Long, String, String) -> MangaSourceData = { id, lang, name ->
    MangaSourceData(id, lang, name)
}
