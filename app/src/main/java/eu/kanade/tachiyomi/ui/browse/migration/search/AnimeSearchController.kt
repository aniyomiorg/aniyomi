package eu.kanade.tachiyomi.ui.browse.migration.search

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchPresenter
import eu.kanade.tachiyomi.ui.browse.migration.MigrationFlags
import uy.kohesive.injekt.injectLazy

class AnimeSearchController(
    private var anime: Anime? = null
) : GlobalAnimeSearchController(anime?.title) {

    private var newAnime: Anime? = null

    override fun createPresenter(): GlobalAnimeSearchPresenter {
        return AnimeSearchPresenter(
            initialQuery,
            anime!!
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(::anime.name, anime)
        outState.putSerializable(::newAnime.name, newAnime)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        anime = savedInstanceState.getSerializable(::anime.name) as? Anime
        newAnime = savedInstanceState.getSerializable(::newAnime.name) as? Anime
    }

    fun migrateAnime(anime: Anime? = null, newAnime: Anime?) {
        anime ?: return
        newAnime ?: return

        (presenter as? AnimeSearchPresenter)?.migrateAnime(anime, newAnime, true)
    }

    fun copyAnime(anime: Anime? = null, newAnime: Anime?) {
        anime ?: return
        newAnime ?: return

        (presenter as? AnimeSearchPresenter)?.migrateAnime(anime, newAnime, false)
    }

    override fun onAnimeClick(anime: Anime) {
        newAnime = anime
        val dialog =
            MigrationDialog(this.anime, newAnime, this)
        dialog.targetController = this
        dialog.showDialog(router)
    }

    override fun onAnimeLongClick(anime: Anime) {
        // Call parent's default click listener
        super.onAnimeClick(anime)
    }

    fun renderIsReplacingAnime(isReplacingAnime: Boolean) {
        if (isReplacingAnime) {
            binding.progress.isVisible = true
        } else {
            binding.progress.isVisible = false
            router.popController(this)
        }
    }

    class MigrationDialog(private val anime: Anime? = null, private val newAnime: Anime? = null, private val callingController: Controller? = null) : DialogController() {

        private val preferences: PreferencesHelper by injectLazy()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val prefValue = preferences.migrateFlags().get()

            val preselected =
                MigrationFlags.getEnabledFlagsPositions(
                    prefValue
                )

            return MaterialDialog(activity!!)
                .title(R.string.migration_dialog_what_to_include)
                .listItemsMultiChoice(
                    items = MigrationFlags.titles.map { resources?.getString(it) as CharSequence },
                    initialSelection = preselected.toIntArray()
                ) { _, positions, _ ->
                    // Save current settings for the next time
                    val newValue =
                        MigrationFlags.getFlagsFromPositions(
                            positions.toTypedArray()
                        )
                    preferences.migrateFlags().set(newValue)
                }
                .positiveButton(R.string.migrate) {
                    if (callingController != null) {
                        if (callingController.javaClass == AnimeSourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? AnimeSearchController)?.migrateAnime(anime, newAnime)
                }
                .negativeButton(R.string.copy) {
                    if (callingController != null) {
                        if (callingController.javaClass == AnimeSourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? AnimeSearchController)?.copyAnime(anime, newAnime)
                }
                .neutralButton(android.R.string.cancel)
        }
    }

    override fun onTitleClick(source: AnimeCatalogueSource) {
        presenter.preferences.lastUsedSource().set(source.id)

        router.pushController(AnimeSourceSearchController(anime, source, presenter.query).withFadeTransaction())
    }
}
