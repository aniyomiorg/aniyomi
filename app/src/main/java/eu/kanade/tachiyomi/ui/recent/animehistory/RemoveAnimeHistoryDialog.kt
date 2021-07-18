package eu.kanade.tachiyomi.ui.recent.animehistory

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.DialogCheckboxView

class RemoveAnimeHistoryDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : RemoveAnimeHistoryDialog.Listener {

    private var anime: Anime? = null

    private var animehistory: AnimeHistory? = null

    constructor(target: T, anime: Anime, animehistory: AnimeHistory) : this() {
        this.anime = anime
        this.animehistory = animehistory
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!

        // Create custom view
        val dialogCheckboxView = DialogCheckboxView(activity).apply {
            setDescription(R.string.dialog_with_checkbox_remove_description_anime)
            setOptionDescription(R.string.dialog_with_checkbox_reset_anime)
        }

        return MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.action_remove)
            .setView(dialogCheckboxView)
            .setPositiveButton(R.string.action_remove) { _, _ -> onPositive(dialogCheckboxView.isChecked()) }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun onPositive(checked: Boolean) {
        val target = targetController as? Listener ?: return
        val anime = anime ?: return
        val animehistory = animehistory ?: return

        target.removeAnimeHistory(anime, animehistory, checked)
    }

    interface Listener {
        fun removeAnimeHistory(anime: Anime, animehistory: AnimeHistory, all: Boolean)
    }
}
