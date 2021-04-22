package eu.kanade.tachiyomi.ui.watcher.setting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.WatcherGeneralSettingsBinding
import eu.kanade.tachiyomi.ui.watcher.WatcherActivity
import eu.kanade.tachiyomi.util.preference.bindToPreference
import uy.kohesive.injekt.injectLazy

/**
 * Sheet to show watcher and viewer preferences.
 */
class WatcherGeneralSettings @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    private val preferences: PreferencesHelper by injectLazy()

    private val binding = WatcherGeneralSettingsBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)

        initGeneralPreferences()
    }

    /**
     * Init general watcher preferences.
     */
    private fun initGeneralPreferences() {
        binding.rotationMode.bindToPreference(preferences.rotation(), 1)
        binding.backgroundColor.bindToIntPreference(preferences.watcherTheme(), R.array.watcher_themes_values)
        binding.showPageNumber.bindToPreference(preferences.showPageNumber())
        binding.fullscreen.bindToPreference(preferences.fullscreen())
        binding.keepscreen.bindToPreference(preferences.keepScreenOn())
        binding.longTap.bindToPreference(preferences.readWithLongTap())
        binding.alwaysShowEpisodeTransition.bindToPreference(preferences.alwaysShowEpisodeTransition())
        binding.pageTransitions.bindToPreference(preferences.pageTransitions())

        // If the preference is explicitly disabled, that means the setting was configured since there is a cutout
        if ((context as WatcherActivity).hasCutout || !preferences.cutoutShort().get()) {
            binding.cutoutShort.isVisible = true
            binding.cutoutShort.bindToPreference(preferences.cutoutShort())
        }
    }
}
