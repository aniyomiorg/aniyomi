package eu.kanade.tachiyomi.ui.animecategory

import androidx.compose.runtime.Composable
import eu.kanade.presentation.category.AnimeCategoryScreen
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController

class AnimeCategoryController : FullComposeController<AnimeCategoryPresenter>() {

    override fun createPresenter() = AnimeCategoryPresenter()

    @Composable
    override fun ComposeContent() {
        AnimeCategoryScreen(
            presenter = presenter,
            navigateUp = router::popCurrentController,
        )
    }
}
