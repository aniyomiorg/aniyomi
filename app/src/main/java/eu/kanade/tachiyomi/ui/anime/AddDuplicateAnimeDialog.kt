package eu.kanade.tachiyomi.ui.anime

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import uy.kohesive.injekt.injectLazy

class AddDuplicateAnimeDialog(bundle: Bundle? = null) : DialogController(bundle) {

    private val sourceManager: AnimeSourceManager by injectLazy()

    private lateinit var libraryAnime: Anime
    private lateinit var onAddToLibrary: () -> Unit

    constructor(
        target: Controller,
        libraryAnime: Anime,
        onAddToLibrary: () -> Unit,
    ) : this() {
        targetController = target

        this.libraryAnime = libraryAnime
        this.onAddToLibrary = onAddToLibrary
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val source = sourceManager.getOrStub(libraryAnime.source)

        return MaterialAlertDialogBuilder(activity!!)
            .setMessage(activity?.getString(R.string.confirm_manga_add_duplicate, source.name))
            .setPositiveButton(activity?.getString(R.string.action_add)) { _, _ ->
                onAddToLibrary()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(activity?.getString(R.string.action_show_anime)) { _, _ ->
                dismissDialog()
                router.pushController(AnimeController(libraryAnime.id!!))
            }
            .setCancelable(true)
            .create()
    }
}
