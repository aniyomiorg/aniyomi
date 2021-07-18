package eu.kanade.tachiyomi.ui.animelib

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.action_edit_cover)
            .setPositiveButton(R.string.action_edit) { _, _ ->
                (targetController as? Listener)?.openAnimeCoverPicker(anime)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.action_delete) { _, _ ->
                (targetController as? Listener)?.deleteAnimeCover(anime)
            }
            .create()
    }

    interface Listener {
        fun deleteAnimeCover(anime: Anime)

        fun openAnimeCoverPicker(anime: Anime)
    }
}
