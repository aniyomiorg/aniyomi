package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.player.Panels
import eu.kanade.tachiyomi.ui.player.controls.components.panels.AudioDelayPanel
import eu.kanade.tachiyomi.ui.player.controls.components.panels.SubtitleDelayPanel
import eu.kanade.tachiyomi.ui.player.controls.components.panels.SubtitleSettingsPanel
import eu.kanade.tachiyomi.ui.player.controls.components.panels.VideoFiltersPanel
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun PlayerPanels(
    panelShown: Panels,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = panelShown,
        label = "panels",
        contentAlignment = Alignment.CenterEnd,
        contentKey = { it.name },
        transitionSpec = {
            fadeIn() + slideInHorizontally { it / 3 } togetherWith fadeOut() + slideOutHorizontally { it / 2 }
        },
        modifier = modifier
    ) { currentPanel ->
        when (currentPanel) {
            Panels.None -> { Box(Modifier.fillMaxHeight()) }
            Panels.SubtitleSettings -> {
                SubtitleSettingsPanel(onDismissRequest)
            }
            Panels.SubtitleDelay -> {
                SubtitleDelayPanel(onDismissRequest)
            }
            Panels.AudioDelay -> {
                AudioDelayPanel(onDismissRequest)
            }
            Panels.VideoFilters -> {
                VideoFiltersPanel(onDismissRequest)
            }
        }
    }
}

val CARDS_MAX_WIDTH = 420.dp
val panelCardsColors: @Composable () -> CardColors = {
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }

    val colors = CardDefaults.cardColors()
    colors.copy(
        containerColor = MaterialTheme.colorScheme.surface.copy(playerPreferences.panelOpacity().get() / 100f),
        disabledContainerColor = MaterialTheme.colorScheme.surfaceDim.copy(playerPreferences.panelOpacity().get() / 100f),
    )
}
