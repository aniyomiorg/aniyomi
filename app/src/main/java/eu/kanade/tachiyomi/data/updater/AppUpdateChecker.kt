package eu.kanade.tachiyomi.data.updater

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import eu.kanade.tachiyomi.util.system.isInstalledFromFDroid
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.release.interactor.GetApplicationRelease
import uy.kohesive.injekt.injectLazy

class AppUpdateChecker(private val context: Context) {
    private val getApplicationRelease: GetApplicationRelease by injectLazy()

    suspend fun checkForUpdates(forceCheck: Boolean = false): GetApplicationRelease.Result {
        return withContext(Dispatchers.IO) {
            getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isPreview = BuildConfig.PREVIEW,
                    commitCount = BuildConfig.COMMIT_COUNT.toInt(),
                    versionName = BuildConfig.VERSION_NAME,
                    repoUrl = GITHUB_REPO,
                    forceCheck = forceCheck,
                ),
            ).also { result ->
                when (result) {
                    is GetApplicationRelease.Result.NewUpdate -> AppUpdateNotifier(context).promptUpdate(result.release)
                    else -> {} // Handle other cases
                }
            }
        }
    }
}

val GITHUB_REPO: String by lazy {
    if (BuildConfig.PREVIEW) {
        "LuftVerbot/kuukiyomi-preview"
    } else {
        "LuftVerbot/kuukiyomi"
    }
}

val RELEASE_TAG: String by lazy {
    if (BuildConfig.PREVIEW) {
        "r${BuildConfig.COMMIT_COUNT}"
    } else {
        "v${BuildConfig.VERSION_NAME}"
    }
}

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
