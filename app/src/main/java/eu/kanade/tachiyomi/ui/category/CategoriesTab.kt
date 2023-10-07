package eu.kanade.tachiyomi.ui.category

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryEvent
import eu.kanade.tachiyomi.ui.category.anime.AnimeCategoryScreenModel
import eu.kanade.tachiyomi.ui.category.anime.animeCategoryTab
import eu.kanade.tachiyomi.ui.category.manga.MangaCategoryEvent
import eu.kanade.tachiyomi.ui.category.manga.MangaCategoryScreenModel
import eu.kanade.tachiyomi.ui.category.manga.mangaCategoryTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest

data class CategoriesTab(
    private val isManga: Boolean = false,
) : Tab() {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 7u,
                title = stringResource(R.string.general_categories),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val animeCategoryScreenModel = rememberScreenModel { AnimeCategoryScreenModel() }
        val mangaCategoryScreenModel = rememberScreenModel { MangaCategoryScreenModel() }

        TabbedScreen(
            titleRes = R.string.general_categories,
            tabs = listOf(
                animeCategoryTab(),
                mangaCategoryTab(),
            ),
            startIndex = 1.takeIf { isManga },
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }

        LaunchedEffect(Unit) {
            mangaCategoryScreenModel.events.collectLatest { event ->
                if (event is MangaCategoryEvent.LocalizedMessage) {
                    context.toast(event.stringRes)
                }
            }
            animeCategoryScreenModel.events.collectLatest { event ->
                if (event is AnimeCategoryEvent.LocalizedMessage) {
                    context.toast(event.stringRes)
                }
            }
        }
    }
}
