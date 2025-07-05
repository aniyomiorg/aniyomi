@file:Suppress("UNUSED", "KotlinConstantConditions")

package eu.kanade.tachiyomi.util.system

import eu.kanade.tachiyomi.BuildConfig

val updaterEnabled: Boolean
    inline get() = BuildConfig.UPDATER_ENABLED

val isDebugBuildType: Boolean
    inline get() = BuildConfig.BUILD_TYPE == "debug"

val isPreviewBuildType: Boolean
    inline get() = BuildConfig.BUILD_TYPE == "preview"

val isReleaseBuildType: Boolean
    inline get() = BuildConfig.BUILD_TYPE == "release"
