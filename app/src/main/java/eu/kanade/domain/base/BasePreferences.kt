package eu.kanade.domain.base

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import eu.kanade.tachiyomi.R
import tachiyomi.core.preference.PreferenceStore

class BasePreferences(
    val context: Context,
    private val preferenceStore: PreferenceStore,
) {

    fun confirmExit() = preferenceStore.getBoolean("pref_confirm_exit", false)

    fun downloadedOnly() = preferenceStore.getBoolean("pref_downloaded_only", false)

    fun incognitoMode() = preferenceStore.getBoolean("incognito_mode", false)

    fun extensionInstaller() = ExtensionInstallerPreference(context, preferenceStore)

    // acra is disabled
    fun acraEnabled() = preferenceStore.getBoolean("acra.enable", false)

    fun deviceHasPip() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    enum class ExtensionInstaller(val titleResId: Int) {
        LEGACY(R.string.ext_installer_legacy),
        PACKAGEINSTALLER(R.string.ext_installer_packageinstaller),
        SHIZUKU(R.string.ext_installer_shizuku),
    }
}
