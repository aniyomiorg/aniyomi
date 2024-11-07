package tachiyomi.domain.release.interactor

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetApplicationRelease(
    private val service: ReleaseService,
    private val preferenceStore: PreferenceStore,
) {

    private val lastChecked: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_app_check"), 0)
    }

    suspend fun await(arguments: Arguments): Result {
        val now = Instant.now()

        // Limit checks to once every 3 days at most
        if (!arguments.forceCheck &&
            now.isBefore(
                Instant.ofEpochMilli(lastChecked.get()).plus(3, ChronoUnit.DAYS),
            )
        ) {
            return Result.NoNewUpdate
        }

        val release = service.latest(arguments.repoUrl)
        lastChecked.set(now.toEpochMilli())

        return if (isNewVersion(arguments, release.version)) {
            Result.NewUpdate(release)
        } else {
            Result.NoNewUpdate
        }
    }

    private fun isNewVersion(arguments: Arguments, versionTag: String): Boolean {
        val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")

        if (arguments.isPreview) {
            // For preview builds, compare commit counts
            return newVersion.toInt() > arguments.commitCount
        } else {
            // For release builds, compare semantic versioning
            val oldVersion = arguments.versionName.replace("[^\\d.]".toRegex(), "")
            val newSemVer = newVersion.split(".").map(String::toInt)
            val oldSemVer = oldVersion.split(".").map(String::toInt)

            return newSemVer.zip(oldSemVer).any { (new, old) -> new > old }
        }
    }

    data class Arguments(
        val isPreview: Boolean,
        val commitCount: Int,
        val versionName: String,
        val repoUrl: String,
        val forceCheck: Boolean = false,
    )

    sealed interface Result {
        data class NewUpdate(val release: Release) : Result
        data object NoNewUpdate : Result
        data object OsTooOld : Result
    }
}
