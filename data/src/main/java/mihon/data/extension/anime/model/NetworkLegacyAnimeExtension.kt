package mihon.data.extension.anime.model

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.serialization.Serializable
import mihon.domain.extension.anime.model.AnimeExtensionStore

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkLegacyAnimeExtension(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val sources: List<Source>?,
) {
    @Serializable
    data class Source(
        val id: Long,
        val lang: String,
        val name: String,
        val baseUrl: String,
    )

    fun toAvailableExtension(store: AnimeExtensionStore, storeBaseUrl: String): AnimeExtension.Available {
        return AnimeExtension.Available(
            name = name.substringAfter("Aniyomi: "),
            pkgName = pkg,
            apkUrl = "$storeBaseUrl/apk/$apk",
            iconUrl = "$storeBaseUrl/icon/$pkg.png",
            libVersion = version.substringBeforeLast('.').toDouble(),
            versionCode = code,
            versionName = version,
            lang = lang,
            isNsfw = nsfw == 1,
            isTorrent = false,
            sources = if (sources.isNullOrEmpty()) {
                listOf(
                    AnimeExtension.Available.AnimeSource(
                        id = 0,
                        name = name,
                        lang = lang,
                        baseUrl = "",
                    ),
                )
            } else {
                sources.map { source ->
                    AnimeExtension.Available.AnimeSource(
                        id = source.id,
                        name = source.name,
                        lang = source.lang,
                        baseUrl = source.baseUrl,
                    )
                }
            },
            store = store,
        )
    }
}
