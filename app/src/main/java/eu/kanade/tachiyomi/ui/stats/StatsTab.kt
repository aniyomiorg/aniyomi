package eu.kanade.tachiyomi.ui.stats

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.stats.anime.animeStatsTab
import eu.kanade.tachiyomi.ui.stats.manga.mangaStatsTab

data class StatsTab(
    private val isManga: Boolean = false,
) : Tab() {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 8u,
                title = stringResource(R.string.label_stats),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        TabbedScreen(
            titleRes = R.string.label_stats,
            tabs = listOf(
                animeStatsTab(),
                mangaStatsTab(),
            ),
            startIndex = 1.takeIf { isManga },

        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
