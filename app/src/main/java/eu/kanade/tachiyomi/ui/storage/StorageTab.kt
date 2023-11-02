package eu.kanade.tachiyomi.ui.storage

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
import eu.kanade.tachiyomi.ui.storage.anime.animeStorageTab
import eu.kanade.tachiyomi.ui.storage.manga.mangaStorageTab

data class StorageTab(
    private val isManga: Boolean = false,
) : Tab() {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 8u,
                title = stringResource(R.string.label_storage),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        TabbedScreen(
            titleRes = R.string.label_storage,
            tabs = listOf(
                animeStorageTab(),
                mangaStorageTab(),
            ),
            startIndex = 1.takeIf { isManga },
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
