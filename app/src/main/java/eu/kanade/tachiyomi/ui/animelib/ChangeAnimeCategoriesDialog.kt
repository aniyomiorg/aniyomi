package eu.kanade.tachiyomi.ui.animelib

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.category.model.Category
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.animecategory.CategoryController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.widget.materialdialogs.QuadStateTextView
import eu.kanade.tachiyomi.widget.materialdialogs.setQuadStateMultiChoiceItems

class ChangeAnimeCategoriesDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : ChangeAnimeCategoriesDialog.Listener {

    private var animes = emptyList<Anime>()
    private var categories = emptyList<Category>()
    private var preselected = intArrayOf()
    private var selected = intArrayOf()

    constructor(
        target: T,
        animes: List<Anime>,
        categories: List<Category>,
        preselected: IntArray,
    ) : this() {
        this.animes = animes
        this.categories = categories
        this.preselected = preselected
        this.selected = preselected
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.action_move_category)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (categories.isNotEmpty()) {
                    setQuadStateMultiChoiceItems(
                        items = categories.map { it.name },
                        isActionList = false,
                        initialSelected = preselected,
                    ) { selections ->
                        selected = selections
                    }
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        val add = selected
                            .mapIndexed { index, value -> if (value == QuadStateTextView.State.CHECKED.ordinal) categories[index] else null }
                            .filterNotNull()
                        val remove = selected
                            .mapIndexed { index, value -> if (value == QuadStateTextView.State.UNCHECKED.ordinal) categories[index] else null }
                            .filterNotNull()
                        (targetController as? Listener)?.updateCategoriesForAnimes(animes, add, remove)
                    }
                    setNeutralButton(R.string.action_edit) { _, _ -> openCategoryController() }
                } else {
                    setMessage(R.string.information_empty_category_dialog)
                    setPositiveButton(R.string.action_edit_anime_categories) { _, _ -> openCategoryController() }
                }
            }
            .create()
    }

    private fun openCategoryController() {
        if (targetController is AnimelibController) {
            val libController = targetController as AnimelibController
            libController.clearSelection()
        }
        router.popCurrentController()
        router.pushController(CategoryController())
    }

    interface Listener {
        fun updateCategoriesForAnimes(animes: List<Anime>, addCategories: List<Category>, removeCategories: List<Category> = emptyList<Category>())
    }
}
