package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.entries.anime.model.Anime

@Suppress("DEPRECATION")
@Serializable
data class BackupAnime(
    // in 1.x some of these values have different names
    @ProtoNumber(1) var source: Long,
    // url is called key in 1.x
    @ProtoNumber(2) var url: String,
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var artist: String? = null,
    @ProtoNumber(5) var author: String? = null,
    @ProtoNumber(6) var description: String? = null,
    @ProtoNumber(7) var genre: List<String> = emptyList(),
    @ProtoNumber(8) var status: Int = 0,
    // thumbnailUrl is called cover in 1.x
    @ProtoNumber(9) var thumbnailUrl: String? = null,
    // @ProtoNumber(10) val customCover: String = "", 1.x value, not used in 0.x
    // @ProtoNumber(11) val lastUpdate: Long = 0, 1.x value, not used in 0.x
    // @ProtoNumber(12) val lastInit: Long = 0, 1.x value, not used in 0.x
    @ProtoNumber(13) var dateAdded: Long = 0,
    // @ProtoNumber(15) val flags: Int = 0, 1.x value, not used in 0.x
    @ProtoNumber(16) var episodes: List<BackupEpisode> = emptyList(),
    @ProtoNumber(17) var categories: List<Long> = emptyList(),
    @ProtoNumber(18) var tracking: List<BackupAnimeTracking> = emptyList(),
    // Bump by 100 for values that are not saved/implemented in 1.x but are used in 0.x
    @ProtoNumber(100) var favorite: Boolean = true,
    @ProtoNumber(101) var episodeFlags: Int = 0,
    // @ProtoNumber(102) var brokenHistory, legacy history model with non-compliant proto number
    @ProtoNumber(103) var viewer_flags: Int = 0,
    @ProtoNumber(104) var history: List<BackupAnimeHistory> = emptyList(),
    @ProtoNumber(105) var updateStrategy: AnimeUpdateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE,
    @ProtoNumber(106) var lastModifiedAt: Long = 0,
    @ProtoNumber(107) var favoriteModifiedAt: Long? = null,
    @ProtoNumber(109) var version: Long = 0,

    // Aniyomi specific values
    @ProtoNumber(500) var backgroundUrl: String? = null,
    // @ProtoNumber(501) Broken, do not use
    @ProtoNumber(502) var parentId: Long? = null,
    @ProtoNumber(503) var id: Long? = null, // Used to associate seasons with parents. Do not use for anything else.
    @ProtoNumber(504) var seasonFlags: Long = 0,
    @ProtoNumber(505) var seasonNumber: Double = -1.0,
    @ProtoNumber(506) var seasonSourceOrder: Long = 0,
    @ProtoNumber(507) var fetchType: FetchType = FetchType.Episodes,
) {
    fun getAnimeImpl(): Anime {
        return Anime.create().copy(
            url = this@BackupAnime.url,
            title = this@BackupAnime.title,
            artist = this@BackupAnime.artist,
            author = this@BackupAnime.author,
            description = this@BackupAnime.description,
            genre = this@BackupAnime.genre,
            status = this@BackupAnime.status.toLong(),
            thumbnailUrl = this@BackupAnime.thumbnailUrl,
            backgroundUrl = this@BackupAnime.backgroundUrl,
            favorite = this@BackupAnime.favorite,
            source = this@BackupAnime.source,
            dateAdded = this@BackupAnime.dateAdded,
            viewerFlags = this@BackupAnime.viewer_flags.toLong(),
            episodeFlags = this@BackupAnime.episodeFlags.toLong(),
            updateStrategy = this@BackupAnime.updateStrategy,
            lastModifiedAt = this@BackupAnime.lastModifiedAt,
            favoriteModifiedAt = this@BackupAnime.favoriteModifiedAt,
            version = this@BackupAnime.version,
            fetchType = this@BackupAnime.fetchType,
            parentId = this@BackupAnime.parentId,
            seasonFlags = this@BackupAnime.seasonFlags,
            seasonNumber = this@BackupAnime.seasonNumber,
            seasonSourceOrder = this@BackupAnime.seasonSourceOrder,
        )
    }
}
