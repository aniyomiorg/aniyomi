package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.CustomAnimeInfo
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.track.anime.model.AnimeTrack

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
    @ProtoNumber(102) var brokenHistory: List<BrokenBackupAnimeHistory> = emptyList(),
    @ProtoNumber(103) var viewer_flags: Int = 0,
    @ProtoNumber(104) var history: List<BackupAnimeHistory> = emptyList(),
    @ProtoNumber(105) var updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,

    @ProtoNumber(602) var customStatus: Int = 0,

    // J2K specific values
    @ProtoNumber(800) var customTitle: String? = null,
    @ProtoNumber(801) var customArtist: String? = null,
    @ProtoNumber(802) var customAuthor: String? = null,
    // skipping 803 due to using duplicate value in previous builds
    @ProtoNumber(804) var customDescription: String? = null,
    @ProtoNumber(805) var customGenre: List<String>? = null,
) {
    fun getAnimeImpl(): Anime {
        return Anime.create().copy(
            url = this@BackupAnime.url,
            // SY -->
            ogTitle = this@BackupAnime.title,
            ogArtist = this@BackupAnime.artist,
            ogAuthor = this@BackupAnime.author,
            ogDescription = this@BackupAnime.description,
            ogGenre = this@BackupAnime.genre,
            ogStatus = this@BackupAnime.status.toLong(),
            // SY <--
            thumbnailUrl = this@BackupAnime.thumbnailUrl,
            favorite = this@BackupAnime.favorite,
            source = this@BackupAnime.source,
            dateAdded = this@BackupAnime.dateAdded,
            viewerFlags = this@BackupAnime.viewer_flags.toLong(),
            episodeFlags = this@BackupAnime.episodeFlags.toLong(),
            updateStrategy = this@BackupAnime.updateStrategy,
        )
    }

    fun getEpisodesImpl(): List<Episode> {
        return episodes.map {
            it.toEpisodeImpl()
        }
    }

    // SY -->
    fun getCustomAnimeInfo(): CustomAnimeInfo? {
        if (customTitle != null ||
            customArtist != null ||
            customAuthor != null ||
            customDescription != null ||
            customGenre != null ||
            customStatus != 0
        ) {
            return CustomAnimeInfo(
                id = 0L,
                title = customTitle,
                author = customAuthor,
                artist = customArtist,
                description = customDescription,
                genre = customGenre,
                status = customStatus.takeUnless { it == 0 }?.toLong(),
            )
        }
        return null
    }
    // SY <--

    fun getTrackingImpl(): List<AnimeTrack> {
        return tracking.map {
            it.getTrackingImpl()
        }
    }

    companion object {
        fun copyFrom(anime: Anime, customAnimeInfo: CustomAnimeInfo?): BackupAnime {
            return BackupAnime(
                url = anime.url,
                // SY -->
                title = anime.ogTitle,
                artist = anime.ogArtist,
                author = anime.ogAuthor,
                description = anime.ogDescription,
                genre = anime.ogGenre.orEmpty(),
                status = anime.ogStatus.toInt(),
                // SY <--
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                source = anime.source,
                dateAdded = anime.dateAdded,
                viewer_flags = anime.skipIntroLength,
                episodeFlags = anime.episodeFlags.toInt(),
                updateStrategy = anime.updateStrategy,
            ).also { backupAnime ->
                customAnimeInfo?.let {
                    backupAnime.customTitle = it.title
                    backupAnime.customArtist = it.artist
                    backupAnime.customAuthor = it.author
                    backupAnime.customDescription = it.description
                    backupAnime.customGenre = it.genre
                    backupAnime.customStatus = it.status?.toInt() ?: 0
                }
            }
        }
    }
}
