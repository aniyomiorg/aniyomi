package eu.kanade.tachiyomi.data.coil

import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.domain.entries.manga.model.hasCustomCover
import eu.kanade.tachiyomi.data.cache.MangaCoverCache
import tachiyomi.domain.entries.manga.model.MangaCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.entries.manga.model.Manga as DomainManga

class MangaKeyer : Keyer<DomainManga> {
    override fun key(data: DomainManga, options: Options): String {
        return if (data.hasCustomCover()) {
            "manga;${data.id};${data.coverLastModified}"
        } else {
            "manga;${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class MangaCoverKeyer(
    private val coverCache: MangaCoverCache = Injekt.get(),
) : Keyer<MangaCover> {
    override fun key(data: MangaCover, options: Options): String {
        return if (coverCache.getCustomCoverFile(data.mangaId).exists()) {
            "manga;${data.mangaId};${data.lastModified}"
        } else {
            "manga;${data.url};${data.lastModified}"
        }
    }
}
