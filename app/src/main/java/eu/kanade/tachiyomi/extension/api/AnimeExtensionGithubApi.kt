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
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.serialization.Serializable
import logcat.LogPriority
import uy.kohesive.injekt.injectLazy
import java.util.Date
import java.util.concurrent.TimeUnit

internal class AnimeExtensionGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    private var requiresFallbackSource = false

    suspend fun findExtensions(): List<AnimeExtension.Available> {
        return withIOContext {
            val response = try {
                networkService.client
                    .newCall(GET("${REPO_URL_PREFIX}index.min.json"))
                    .await()
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "Failed to get extensions from GitHub" }
                requiresFallbackSource = true

                networkService.client
                    .newCall(GET("${FALLBACK_REPO_URL_PREFIX}index.min.json"))
                    .await()
            }

            val extensions = response
                .parseAs<List<AnimeExtensionJsonObject>>()
                .toExtensions()

            // Sanity check - a small number of extensions probably means something broke
            // with the repo generator
            if (extensions.size < 10) {
                throw Exception()
            }

            extensions
        }
    }

    suspend fun checkForUpdates(context: Context): List<AnimeExtension.Installed>? {
        // Limit checks to once a day at most
        if (Date().time < preferences.lastAnimeExtCheck().get() + TimeUnit.DAYS.toMillis(1)) {
            return null
        }

        val extensions = findExtensions().also { preferences.lastAnimeExtCheck().set(Date().time) }

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
                    // need to implement new extension stuff
                    hasReadme = false,
                    hasChangelog = false,
                    apkName = it.apk,
                    iconUrl = "${getUrlPrefix()}icon/${it.apk.replace(".apk", ".png")}",
                )
            }
    }

    fun getApkUrl(extension: AnimeExtension.Available): String {
        return "${getUrlPrefix()}apk/${extension.apkName}"
    }

    private fun getUrlPrefix(): String {
        return if (requiresFallbackSource) {
            FALLBACK_REPO_URL_PREFIX
        } else {
            REPO_URL_PREFIX
        }
    }
}

private const val REPO_URL_PREFIX = "https://raw.githubusercontent.com/jmir1/aniyomi-extensions/repo/"
private const val FALLBACK_REPO_URL_PREFIX = "https://fastly.jsdelivr.net/gh/jmir1/aniyomi-extensions@repo/"

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
