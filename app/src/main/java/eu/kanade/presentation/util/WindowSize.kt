package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TvUiMode
import eu.kanade.tachiyomi.util.system.isTabletUi
import eu.kanade.tachiyomi.util.system.isTvUiMode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
@ReadOnlyComposable
fun isTabletUi(): Boolean {
    return LocalConfiguration.current.isTabletUi()
}

@Composable
@ReadOnlyComposable
fun isTvUi(): Boolean {
    return when (Injekt.get<UiPreferences>().tvUiMode().get()) {
        TvUiMode.ALWAYS -> true
        TvUiMode.NEVER -> false
        TvUiMode.AUTOMATIC -> LocalConfiguration.current.isTvUiMode()
    }
}
