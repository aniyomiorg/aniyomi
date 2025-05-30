package eu.kanade.tachiyomi.data.updater

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.release.interactor.GetApplicationRelease
import uy.kohesive.injekt.injectLazy

class AppUpdateChecker(private val context: Context) {
    private val getApplicationRelease: GetApplicationRelease by injectLazy()

    suspend fun checkForUpdates(forceCheck: Boolean = false): GetApplicationRelease.Result {
        return withContext(Dispatchers.IO) {
            getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isPreviewBuildType,
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    forceCheck,
                ),
            )

            when (result) {
                is GetApplicationRelease.Result.NewUpdate -> AppUpdateNotifier(context).promptUpdate(
                    result.release,
                )
                else -> {}
            }
        }
    }
}

val GITHUB_REPO: String by lazy {
    if (isPreviewBuildType) {
        "Animetailapp/animetail-preview"
    } else {
        "Animetailapp/Animetail"
    }
}

val RELEASE_TAG: String by lazy {
    if (isPreviewBuildType) {
        "r${BuildConfig.COMMIT_COUNT}"
    } else {
        "v${BuildConfig.VERSION_NAME}"
    }
}

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
