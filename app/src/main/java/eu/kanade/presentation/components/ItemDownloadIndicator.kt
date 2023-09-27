package eu.kanade.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.presentation.core.components.material.IconButtonTokens
import uy.kohesive.injekt.injectLazy

internal fun Modifier.commonClickable(
    enabled: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) = composed {
    val haptic = LocalHapticFeedback.current

    this.combinedClickable(
        enabled = enabled,
        onLongClick = {
            onLongClick()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onClick = onClick,
        role = Role.Button,
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(
            bounded = false,
            radius = IconButtonTokens.StateLayerSize / 2,
        ),
    )
}

internal val IndicatorSize = 26.dp
internal val IndicatorPadding = 2.dp

// To match composable parameter name when used later
internal val IndicatorStrokeWidth = IndicatorPadding

internal val IndicatorModifier = Modifier
    .size(IndicatorSize)
    .padding(IndicatorPadding)
internal val ArrowModifier = Modifier
    .size(IndicatorSize - 7.dp)

internal val preferences: DownloadPreferences by injectLazy()
