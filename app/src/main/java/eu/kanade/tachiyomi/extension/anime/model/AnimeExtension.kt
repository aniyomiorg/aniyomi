package eu.kanade.tachiyomi.extension.anime.model

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.animesource.AnimeSource
import tachiyomi.domain.source.anime.model.StubAnimeSource

sealed class AnimeExtension {

    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val libVersion: Double
    abstract val lang: String?
    abstract val isNsfw: Boolean
    abstract val isTorrent: Boolean

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        override val isTorrent: Boolean,
        val pkgFactory: String?,
        val sources: List<AnimeSource>,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        val isShared: Boolean,
        val repoUrl: String? = null,
    ) : AnimeExtension()

    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        override val isTorrent: Boolean,
        val sources: List<AnimeSource>,
        val apkName: String,
        val iconUrl: String,
        val repoUrl: String,
    ) : AnimeExtension() {

        data class AnimeSource(
            val id: Long,
            val lang: String,
            val name: String,
            val baseUrl: String,
        ) {
            fun toStubSource(): StubAnimeSource {
                return StubAnimeSource(
                    id = this.id,
                    lang = this.lang,
                    name = this.name,
                )
            }
        }
    }

    data class Untrusted(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        val signatureHash: String,
        override val lang: String? = null,
        override val isNsfw: Boolean = false,
        override val isTorrent: Boolean = false,
    ) : AnimeExtension()
}
