package eu.kanade.tachiyomi.ui.anime.episode

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.interactor.SetAnimeDefaultEpisodeFlags
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.getSerializableCompat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.DialogCheckboxView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

class SetEpisodeSettingsDialog(bundle: Bundle? = null) : DialogController(bundle) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val libraryPreferences: LibraryPreferences by injectLazy()
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags by injectLazy()

    constructor(anime: Anime) : this(
        bundleOf(ANIME_KEY to anime),
    )

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val view = DialogCheckboxView(activity!!).apply {
            setDescription(R.string.confirm_set_chapter_settings)
            setOptionDescription(R.string.also_set_episode_settings_for_library)
        }

        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.episode_settings)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                libraryPreferences.setEpisodeSettingsDefault(args.getSerializableCompat(ANIME_KEY)!!)
                if (view.isChecked()) {
                    scope.launch {
                        setAnimeDefaultEpisodeFlags.awaitAll()
                    }
                }

                activity?.toast(R.string.episode_settings_updated)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

private const val ANIME_KEY = "anime"
