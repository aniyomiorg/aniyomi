package eu.kanade.tachiyomi.animeextension.api

import android.content.Context
import eu.kanade.tachiyomi.animeextension.AnimeExtensionManager
import eu.kanade.tachiyomi.animeextension.model.AnimeExtension
import eu.kanade.tachiyomi.animeextension.model.AnimeLoadResult
import eu.kanade.tachiyomi.animeextension.model.AvailableAnimeSources
import eu.kanade.tachiyomi.animeextension.util.AnimeExtensionLoader
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.core.preference.PreferenceStore
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
    private val preferenceStore: PreferenceStore by injectLazy()
    private val lastExtCheck: Preference<Long> by lazy {
        preferenceStore.getLong("last_ext_check", 0)
    }

    private val animeExtensionManager: AnimeExtensionManager by injectLazy()

    private var requiresFallbackSource = false

    suspend fun findExtensions(): List<AnimeExtension.Available> {
        return withIOContext {
            val githubResponse = if (requiresFallbackSource) {
                null
            } else {
                try {
                    networkService.client
                        .newCall(GET("${REPO_URL_PREFIX}index.min.json"))
                        .await()
                } catch (e: Throwable) {
                    logcat(LogPriority.ERROR, e) { "Failed to get extensions from GitHub" }
                    requiresFallbackSource = true
                    null
                }
            }

            val response = githubResponse ?: run {
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

    suspend fun checkForUpdates(context: Context, fromAvailableExtensionList: Boolean = false): List<AnimeExtension.Installed>? {
        // Limit checks to once a day at most
        if (fromAvailableExtensionList.not() && Date().time < lastExtCheck.get() + TimeUnit.DAYS.toMillis(1)) {
            return null
        }

        val extensions = if (fromAvailableExtensionList) {
            animeExtensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also { lastExtCheck.set(Date().time) }
        }

        val installedExtensions = AnimeExtensionLoader.loadExtensions(context)
            .filterIsInstance<AnimeLoadResult.Success>()
            .map { it.extension }

        val extensionsWithUpdate = mutableListOf<AnimeExtension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue

            val hasUpdate = installedExt.isUnofficial.not() && (availableExt.versionCode > installedExt.versionCode)
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        return extensionsWithUpdate
    }

    private fun List<AnimeExtensionJsonObject>.toExtensions(): List<AnimeExtension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= AnimeExtensionLoader.LIB_VERSION_MIN && libVersion <= AnimeExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                AnimeExtension.Available(
                    name = it.name.substringAfter("Aniyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    hasReadme = it.hasReadme == 1,
                    hasChangelog = it.hasChangelog == 1,
                    sources = it.sources?.toAnimeExtensionSources() ?: emptyList(),
                    apkName = it.apk,
                    iconUrl = "${getUrlPrefix()}icon/${it.apk.replace(".apk", ".png")}",
                )
            }
    }

    private fun List<AnimeExtensionSourceJsonObject>.toAnimeExtensionSources(): List<AvailableAnimeSources> {
        return this.map {
            AvailableAnimeSources(
                id = it.id,
                lang = it.lang,
                name = it.name,
                baseUrl = it.baseUrl,
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

private fun AnimeExtensionJsonObject.extractLibVersion(): Double {
    return version.substringBeforeLast('.').toDouble()
}

private const val REPO_URL_PREFIX = "https://raw.githubusercontent.com/jmir1/aniyomi-extensions/repo/"
private const val FALLBACK_REPO_URL_PREFIX = "https://gcore.jsdelivr.net/gh/jmir1/aniyomi-extensions@repo/"

@Serializable
private data class AnimeExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val hasReadme: Int = 0,
    val hasChangelog: Int = 0,
    val sources: List<AnimeExtensionSourceJsonObject>?,
)

@Serializable
private data class AnimeExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)
