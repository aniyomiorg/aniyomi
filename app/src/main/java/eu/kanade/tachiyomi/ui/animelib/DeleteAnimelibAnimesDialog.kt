package eu.kanade.tachiyomi.ui.animelib

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class DeleteAnimelibAnimesDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : DeleteAnimelibAnimesDialog.Listener {

    private var animes = emptyList<Anime>()

    constructor(target: T, animes: List<Anime>) : this() {
        this.animes = animes
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.action_remove)
            .listItemsMultiChoice(
                R.array.delete_selected_animes,
                initialSelection = intArrayOf(0)
            ) { _, selections, _ ->
                val deleteFromAnimelib = 0 in selections
                val deleteChapters = 1 in selections
                (targetController as? Listener)?.deleteAnimes(animes, deleteFromAnimelib, deleteChapters)
            }
            .positiveButton(android.R.string.ok)
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun deleteAnimes(animes: List<Anime>, deleteFromAnimelib: Boolean, deleteChapters: Boolean)
    }
}
