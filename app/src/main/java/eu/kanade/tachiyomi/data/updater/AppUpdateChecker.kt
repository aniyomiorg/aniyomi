package eu.kanade.tachiyomi.data.updater

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.system.isInstalledFromFDroid
import kotlinx.serialization.json.Json
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.domain.release.interactor.GetApplicationRelease
import uy.kohesive.injekt.injectLazy
import java.util.Date
import kotlin.time.Duration.Companion.days

class AppUpdateChecker {

    private val getApplicationRelease: GetApplicationRelease by injectLazy()


    suspend fun checkForUpdate(context: Context, forceCheck: Boolean = false): GetApplicationRelease.Result {
        return withIOContext {
            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    BuildConfig.PREVIEW,
                    context.isInstalledFromFDroid(),
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    forceCheck,
                ),
            )

            when (result) {
                is GetApplicationRelease.Result.NewUpdate -> AppUpdateNotifier(context).promptUpdate(result.release)
                is GetApplicationRelease.Result.ThirdPartyInstallation -> AppUpdateNotifier(context).promptFdroidUpdate()
                else -> {}
            }

            result
        }
    }
}

val GITHUB_REPO: String by lazy {
    if (BuildConfig.PREVIEW) {
        "aniyomiorg/aniyomi-preview"
    } else {
        "aniyomiorg/aniyomi"
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
