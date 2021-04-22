package eu.kanade.tachiyomi.ui.anime.episode

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.episode.EpisodeSettingsHelper
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.DialogCheckboxView

class SetEpisodeSettingsDialog(bundle: Bundle? = null) : DialogController(bundle) {

    constructor(anime: Anime) : this(
        bundleOf(MANGA_KEY to anime)
    )

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val view = DialogCheckboxView(activity!!).apply {
            setDescription(R.string.confirm_set_chapter_settings)
            setOptionDescription(R.string.also_set_chapter_settings_for_library)
        }

        return MaterialDialog(activity!!)
            .title(R.string.chapter_settings)
            .customView(
                view = view,
                horizontalPadding = true
            )
            .positiveButton(android.R.string.ok) {
                EpisodeSettingsHelper.setGlobalSettings(args.getSerializable(MANGA_KEY)!! as Anime)
                if (view.isChecked()) {
                    EpisodeSettingsHelper.updateAllAnimesWithGlobalDefaults()
                }

                activity?.toast(activity!!.getString(R.string.chapter_settings_updated))
            }
            .negativeButton(android.R.string.cancel)
    }

    private companion object {
        const val MANGA_KEY = "anime"
    }
}
