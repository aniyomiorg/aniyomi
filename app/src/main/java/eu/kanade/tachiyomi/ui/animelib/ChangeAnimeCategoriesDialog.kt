package eu.kanade.tachiyomi.ui.animelib

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.animecategory.CategoryController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction

class ChangeAnimeCategoriesDialog<T>(bundle: Bundle? = null) :
    DialogController(bundle) where T : Controller, T : ChangeAnimeCategoriesDialog.Listener {

    private var animes = emptyList<Anime>()
    private var categories = emptyList<Category>()
    private var preselected = emptyArray<Int>()

    constructor(
        target: T,
        animes: List<Anime>,
        categories: List<Category>,
        preselected: Array<Int>
    ) : this() {
        this.animes = animes
        this.categories = categories
        this.preselected = preselected
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(activity!!)
            .setTitle(R.string.action_move_category)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (categories.isNotEmpty()) {
                    val selected = categories
                        .mapIndexed { i, _ -> preselected.contains(i) }
                        .toBooleanArray()
                    setMultiChoiceItems(categories.map { it.name }.toTypedArray(), selected) { _, which, checked ->
                        selected[which] = checked
                    }
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        val newCategories = categories.filterIndexed { i, _ -> selected[i] }
                        (targetController as? Listener)?.updateCategoriesForAnimes(animes, newCategories)
                    }
                } else {
                    setMessage(R.string.information_empty_category_dialog)
                    setPositiveButton(R.string.action_edit_categories) { _, _ ->
                        if (targetController is AnimelibController) {
                            val libController = targetController as AnimelibController
                            libController.clearSelection()
                        }
                        router.popCurrentController()
                        router.pushController(CategoryController().withFadeTransaction())
                    }
                }
            }
            .create()
    }

    interface Listener {
        fun updateCategoriesForAnimes(animes: List<Anime>, categories: List<Category>)
    }
}
