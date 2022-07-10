package eu.kanade.tachiyomi.ui.anime.episode

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.episode.EpisodeSettingsHelper
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.DialogCheckboxView

class SetEpisodeSettingsDialog(bundle: Bundle? = null) : DialogController(bundle) {

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
                EpisodeSettingsHelper.setGlobalSettings(args.getSerializable(ANIME_KEY)!! as Anime)
                if (view.isChecked()) {
                    EpisodeSettingsHelper.updateAllAnimesWithGlobalDefaults()
                }

                activity?.toast(activity!!.getString(R.string.episode_settings_updated))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}

private const val ANIME_KEY = "anime"
