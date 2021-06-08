package eu.kanade.tachiyomi.ui.animelib

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ChangeAnimeCoverDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : ChangeAnimeCoverDialog.Listener {

    private lateinit var anime: Anime

    constructor(target: T, anime: Anime) : this() {
        targetController = target
        this.anime = anime
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.action_edit_cover)
            .positiveButton(R.string.action_edit) {
                (targetController as? Listener)?.openAnimeCoverPicker(anime)
            }
            .negativeButton(android.R.string.cancel)
            .neutralButton(R.string.action_delete) {
                (targetController as? Listener)?.deleteAnimeCover(anime)
            }
    }

    interface Listener {
        fun deleteAnimeCover(anime: Anime)

        fun openAnimeCoverPicker(anime: Anime)
    }
}
