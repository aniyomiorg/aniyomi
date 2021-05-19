package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ConfirmDeleteEpisodesDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : ConfirmDeleteEpisodesDialog.Listener {

    private var episodesToDelete = emptyList<AnimeUpdatesItem>()

    constructor(target: T, episodesToDelete: List<AnimeUpdatesItem>) : this() {
        this.episodesToDelete = episodesToDelete
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .message(R.string.confirm_delete_episodes)
            .positiveButton(android.R.string.ok) {
                (targetController as? Listener)?.deleteEpisodes(episodesToDelete)
            }
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun deleteEpisodes(episodesToDelete: List<AnimeUpdatesItem>)
    }
}
