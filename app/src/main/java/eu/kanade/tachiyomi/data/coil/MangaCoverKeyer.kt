package eu.kanade.tachiyomi.data.coil

import coil.key.Keyer
import coil.request.Options
import eu.kanade.domain.manga.model.MangaCover
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.toDomainManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.domain.manga.model.Manga as DomainManga

class MangaKeyer : Keyer<Manga> {
    override fun key(data: Manga, options: Options): String {
        return if (data.toDomainManga()!!.hasCustomCover()) {
            "manga;${data.id};${data.cover_last_modified}"
        } else {
            "manga;${data.thumbnail_url};${data.cover_last_modified}"
        }
    }
}

class DomainMangaKeyer : Keyer<DomainManga> {
    override fun key(data: DomainManga, options: Options): String {
        return if (data.hasCustomCover()) {
            "manga;${data.id};${data.coverLastModified}"
        } else {
            "manga;${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class MangaCoverKeyer : Keyer<MangaCover> {
    override fun key(data: MangaCover, options: Options): String {
        return if (Injekt.get<CoverCache>().getCustomCoverFile(data.mangaId).exists()) {
            "manga;${data.mangaId};${data.lastModified}"
        } else {
            "manga;${data.url};${data.lastModified}"
        }
    }
}
