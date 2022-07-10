package eu.kanade.tachiyomi.ui.animelib

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class DeleteAnimelibAnimesDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : DeleteAnimelibAnimesDialog.Listener {

    private var animes = emptyList<Anime>()

    constructor(target: T, animes: List<Anime>) : this() {
        this.animes = animes
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val canDeleteEpisodes = animes.any { !it.isLocal() }
        val items = when (canDeleteEpisodes) {
            true -> listOf(
                R.string.anime_from_library,
                R.string.downloaded_episodes,
            )
            false -> listOf(R.string.anime_from_library)
        }
            .map { resources!!.getString(it) }
            .toTypedArray()

        val selected = items
            .map { false }
            .toBooleanArray()
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.action_remove)
            .setMultiChoiceItems(items, selected) { _, which, checked ->
                selected[which] = checked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val deleteFromLibrary = selected[0]
                val deleteEpisodes = canDeleteEpisodes && selected[1]
                (targetController as? Listener)?.deleteAnimes(animes, deleteFromLibrary, deleteEpisodes)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    interface Listener {
        fun deleteAnimes(animes: List<Anime>, deleteFromAnimelib: Boolean, deleteEpisodes: Boolean)
    }
}
