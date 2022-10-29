package eu.kanade.tachiyomi.data.preference

import eu.kanade.tachiyomi.R

const val DEVICE_ONLY_ON_WIFI = "wifi"
const val DEVICE_NETWORK_NOT_METERED = "network_not_metered"
const val DEVICE_CHARGING = "ac"
const val DEVICE_BATTERY_NOT_LOW = "battery_not_low"

const val MANGA_NON_COMPLETED = "manga_ongoing"
const val MANGA_HAS_UNREAD = "manga_fully_read"
const val MANGA_NON_READ = "manga_started"

const val FLAG_CATEGORIES = "1"
const val FLAG_CHAPTERS = "2"
const val FLAG_HISTORY = "4"
const val FLAG_TRACK = "8"
const val FLAG_SETTINGS = "10"

/**
 * This class stores the values for the preferences in the application.
 */
object PreferenceValues {

    enum class TappingInvertMode(val shouldInvertHorizontal: Boolean = false, val shouldInvertVertical: Boolean = false) {
        NONE,
        HORIZONTAL(shouldInvertHorizontal = true),
        VERTICAL(shouldInvertVertical = true),
        BOTH(shouldInvertHorizontal = true, shouldInvertVertical = true),
    }

    enum class ReaderHideThreshold(val threshold: Int) {
        HIGHEST(5),
        HIGH(13),
        LOW(31),
        LOWEST(47),
    }

    enum class ExtensionInstaller(val titleResId: Int) {
        LEGACY(R.string.ext_installer_legacy),
        PACKAGEINSTALLER(R.string.ext_installer_packageinstaller),
        SHIZUKU(R.string.ext_installer_shizuku),
    }
}
