package eu.kanade.tachiyomi.ui.category

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource

data object CategoriesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 7u,
                title = stringResource(AYMR.strings.general_categories),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    private val switchToMangaCategoryTabChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    fun showMangaCategory() {
        switchToMangaCategoryTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val animeCategoryScreenModel = rememberScreenModel { AnimeCategoryScreenModel() }
        val mangaCategoryScreenModel = rememberScreenModel { MangaCategoryScreenModel() }

        val tabs = persistentListOf(
            animeCategoryTab(),
            mangaCategoryTab(),
        )

        val state = rememberPagerState { tabs.size }

        TabbedScreen(
            titleRes = AYMR.strings.general_categories,
            tabs = tabs,
            state = state,
        )
        LaunchedEffect(Unit) {
            switchToMangaCategoryTabChannel.receiveAsFlow()
                .collectLatest { state.scrollToPage(1) }
        }

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
