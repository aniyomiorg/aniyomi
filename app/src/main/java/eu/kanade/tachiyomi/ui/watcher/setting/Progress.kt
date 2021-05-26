package eu.kanade.tachiyomi.ui.watcher.setting

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class Progress(val prefValue: Int, val floatValue: Float, @StringRes val stringRes: Int, val flagValue: Int) {
    // TODO Default icon
    ZERO(0, 0.80f, R.string.pref_progress_80, 0x00000000),
    ONE(1, 0.85f, R.string.pref_progress_85, 0x00000008),
    TWO(2, 0.90f, R.string.pref_progress_90, 0x00000010),
    THREE(3, 0.95f, R.string.pref_progress_95, 0x00000018),
    FOUR(4, 1.00f, R.string.pref_progress_100, 0x00000020);

    companion object {
        const val MASK = 0x00000038

        fun fromPreference(preference: Int?): Progress = values().find { it.flagValue == preference } ?: ONE
    }
}
