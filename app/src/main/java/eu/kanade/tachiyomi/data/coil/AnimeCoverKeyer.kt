package eu.kanade.tachiyomi.data.coil

import coil.key.Keyer
import coil.request.Options
import eu.kanade.domain.anime.model.AnimeCover
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.domain.anime.model.Anime as DomainAnime

class AnimeKeyer : Keyer<Anime> {
    override fun key(data: Anime, options: Options): String {
        return if (data.toDomainAnime()!!.hasCustomCover()) {
            "anime;${data.id};${data.cover_last_modified}"
        } else {
            "anime;${data.thumbnail_url};${data.cover_last_modified}"
        }
    }
}

class DomainAnimeKeyer : Keyer<DomainAnime> {
    override fun key(data: DomainAnime, options: Options): String {
        return if (data.hasCustomCover()) {
            "anime;${data.id};${data.coverLastModified}"
        } else {
            "anime;${data.thumbnailUrl};${data.coverLastModified}"
        }
    }
}

class AnimeCoverKeyer : Keyer<AnimeCover> {
    override fun key(data: AnimeCover, options: Options): String {
        return if (Injekt.get<AnimeCoverCache>().getCustomCoverFile(data.animeId).exists()) {
            "anime;${data.animeId};${data.lastModified}"
        } else {
            "anime;${data.url};${data.lastModified}"
        }
    }
}
