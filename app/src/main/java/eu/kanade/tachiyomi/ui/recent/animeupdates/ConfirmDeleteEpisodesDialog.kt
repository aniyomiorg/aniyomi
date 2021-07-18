package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        return MaterialAlertDialogBuilder(activity!!)
            .setMessage(R.string.confirm_delete_episodes)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                (targetController as? Listener)?.deleteEpisodes(episodesToDelete)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    interface Listener {
        fun deleteEpisodes(episodesToDelete: List<AnimeUpdatesItem>)
    }
}
