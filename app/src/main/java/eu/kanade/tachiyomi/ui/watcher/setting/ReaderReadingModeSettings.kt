package eu.kanade.tachiyomi.ui.watcher.setting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.databinding.WatcherReadingModeSettingsBinding
import eu.kanade.tachiyomi.ui.watcher.WatcherActivity
import eu.kanade.tachiyomi.ui.watcher.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.watcher.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.preference.bindToPreference
import kotlinx.coroutines.flow.launchIn
import uy.kohesive.injekt.injectLazy

/**
 * Sheet to show watcher and viewer preferences.
 */
class WatcherReadingModeSettings @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    private val preferences: PreferencesHelper by injectLazy()

    private val binding = WatcherReadingModeSettingsBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)

        initGeneralPreferences()

        when ((context as WatcherActivity).viewer) {
            is PagerViewer -> initPagerPreferences()
            is WebtoonViewer -> initWebtoonPreferences()
        }
    }

    /**
     * Init general watcher preferences.
     */
    private fun initGeneralPreferences() {
        binding.viewer.onItemSelectedListener = { position ->
            (context as WatcherActivity).presenter.setAnimeViewer(position)

            val animeViewer = (context as WatcherActivity).presenter.getAnimeViewer()
            if (animeViewer == ReadingModeType.WEBTOON.prefValue || animeViewer == ReadingModeType.CONTINUOUS_VERTICAL.prefValue) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        binding.viewer.setSelection((context as WatcherActivity).presenter.anime?.viewer ?: 0)
    }

    /**
     * Init the preferences for the pager watcher.
     */
    private fun initPagerPreferences() {
        binding.webtoonPrefsGroup.root.isVisible = false
        binding.pagerPrefsGroup.root.isVisible = true

        binding.pagerPrefsGroup.tappingPrefsGroup.isVisible = preferences.readWithTapping().get()

        binding.pagerPrefsGroup.tappingInverted.bindToPreference(preferences.pagerNavInverted())

        binding.pagerPrefsGroup.pagerNav.bindToPreference(preferences.navigationModePager())
        binding.pagerPrefsGroup.scaleType.bindToPreference(preferences.imageScaleType(), 1)
        binding.pagerPrefsGroup.zoomStart.bindToPreference(preferences.zoomStart(), 1)
        binding.pagerPrefsGroup.cropBorders.bindToPreference(preferences.cropBorders())

        // Makes so that dual page invert gets hidden away when turning of dual page split
        binding.pagerPrefsGroup.dualPageSplit.bindToPreference(preferences.dualPageSplitPaged())
        preferences.dualPageSplitPaged()
            .asImmediateFlow { binding.pagerPrefsGroup.dualPageInvert.isVisible = it }
            .launchIn((context as WatcherActivity).lifecycleScope)
        binding.pagerPrefsGroup.dualPageInvert.bindToPreference(preferences.dualPageInvertPaged())
    }

    /**
     * Init the preferences for the webtoon watcher.
     */
    private fun initWebtoonPreferences() {
        binding.pagerPrefsGroup.root.isVisible = false
        binding.webtoonPrefsGroup.root.isVisible = true

        binding.webtoonPrefsGroup.tappingPrefsGroup.isVisible = preferences.readWithTapping().get()

        binding.webtoonPrefsGroup.tappingInverted.bindToPreference(preferences.webtoonNavInverted())

        binding.webtoonPrefsGroup.webtoonNav.bindToPreference(preferences.navigationModeWebtoon())
        binding.webtoonPrefsGroup.cropBordersWebtoon.bindToPreference(preferences.cropBordersWebtoon())
        binding.webtoonPrefsGroup.webtoonSidePadding.bindToIntPreference(preferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)

        // Makes so that dual page invert gets hidden away when turning of dual page split
        binding.webtoonPrefsGroup.dualPageSplit.bindToPreference(preferences.dualPageSplitWebtoon())
        preferences.dualPageSplitWebtoon()
            .asImmediateFlow { binding.webtoonPrefsGroup.dualPageInvert.isVisible = it }
            .launchIn((context as WatcherActivity).lifecycleScope)
        binding.webtoonPrefsGroup.dualPageInvert.bindToPreference(preferences.dualPageInvertWebtoon())
    }
}
