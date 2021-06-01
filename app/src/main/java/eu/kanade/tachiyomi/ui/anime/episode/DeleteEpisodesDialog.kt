package eu.kanade.tachiyomi.ui.anime.episode

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class DeleteEpisodesDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : DeleteEpisodesDialog.Listener {

    constructor(target: T) : this() {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .message(R.string.confirm_delete_episodes)
            .positiveButton(android.R.string.ok) {
                (targetController as? Listener)?.deleteEpisodes()
            }
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun deleteEpisodes()
    }
}
