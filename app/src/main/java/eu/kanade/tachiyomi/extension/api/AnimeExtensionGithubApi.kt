package eu.kanade.tachiyomi.extension.api

import android.content.Context
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.model.AnimeExtension
import eu.kanade.tachiyomi.extension.model.AnimeLoadResult
import eu.kanade.tachiyomi.extension.util.AnimeExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.serialization.Serializable
import uy.kohesive.injekt.injectLazy
import java.util.Date

internal class AnimeExtensionGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    suspend fun findExtensions(): List<AnimeExtension.Available> {
        return withIOContext {
            networkService.client
                .newCall(GET("${REPO_URL_PREFIX}index.min.json"))
                .await()
                .parseAs<List<AnimeExtensionJsonObject>>()
                .toExtensions()
        }
    }

    suspend fun checkForUpdates(context: Context): List<AnimeExtension.Installed> {
        val extensions = findExtensions()

        preferences.lastAnimeExtCheck().set(Date().time)

        val installedExtensions = AnimeExtensionLoader.loadExtensions(context)
            .filterIsInstance<AnimeLoadResult.Success>()
            .map { it.extension }

        val extensionsWithUpdate = mutableListOf<AnimeExtension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue

            val hasUpdate = availableExt.versionCode > installedExt.versionCode
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        return extensionsWithUpdate
    }

    private fun List<AnimeExtensionJsonObject>.toExtensions(): List<AnimeExtension.Available> {
        return this
            .filter {
                val libVersion = it.version.substringBeforeLast('.').toDouble()
                libVersion >= AnimeExtensionLoader.LIB_VERSION_MIN && libVersion <= AnimeExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                AnimeExtension.Available(
                    name = it.name.substringAfter("Aniyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    apkName = it.apk,
                    iconUrl = "${REPO_URL_PREFIX}icon/${it.apk.replace(".apk", ".png")}"
                )
            }
    }

    fun getApkUrl(extension: AnimeExtension.Available): String {
        return "${REPO_URL_PREFIX}apk/${extension.apkName}"
    }
}

private const val REPO_URL_PREFIX = "https://raw.githubusercontent.com/jmir1/aniyomi-extensions/repo/"

@Serializable
data class AnimeExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val version: String,
    val code: Long,
    val lang: String,
    val nsfw: Int,
)
