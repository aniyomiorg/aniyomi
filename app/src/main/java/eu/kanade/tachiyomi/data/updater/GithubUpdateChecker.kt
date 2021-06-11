package eu.kanade.tachiyomi.data.updater

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy

class GithubUpdateChecker {

    private val networkService: NetworkHelper by injectLazy()

    private val repo: String by lazy {
        if (BuildConfig.DEBUG) {
            "jmir1/aniyomi-preview"
        } else {
            "jmir1/aniyomi"
        }
    }

    suspend fun checkForUpdate(): GithubUpdateResult {
        return withIOContext {
            networkService.client
                .newCall(GET("https://api.github.com/repos/$repo/releases/latest"))
                .await()
                .parseAs<GithubRelease>()
                .let {
                    // Check if latest version is different from current version
                    if (isNewVersion(it.version)) {
                        GithubUpdateResult.NewUpdate(it)
                    } else {
                        GithubUpdateResult.NoNewUpdate
                    }
                }
        }
    }

    private fun isNewVersion(versionTag: String): Boolean {
        // Removes prefixes like "r" or "v"
        val newVersion = versionTag.replace("[^\\d.\\-mi]".toRegex(), "")

        return if (BuildConfig.DEBUG) {
            // Preview builds: based on releases in "jmir1/tachiyomi-preview" repo
            // tagged as something like "r1234"
            newVersion.toInt() > BuildConfig.COMMIT_COUNT.toInt()
        } else {
            // Release builds: based on releases in "jmir1/aniyomi" repo
            // tagged as something like "v0.1.2-mi"
            newVersion != BuildConfig.VERSION_NAME
        }
    }
}
