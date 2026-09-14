package tachiyomi.presentation.core.util

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA

private val TvFocusBorderWidth = 2.dp
private val TvFocusBorderShape = RoundedCornerShape(8.dp)

/**
 * Draws a visible focus indicator on Android TV, driven by the [interactionSource] that's
 * already passed to this element's own clickable/combinedClickable/selectable modifier.
 *
 * Do NOT add a second, independent `.focusable()` here: `clickable`/`combinedClickable`/
 * `selectable` already register their own focus target, and Compose skips a focusable
 * ancestor that has a focusable descendant (or vice versa) — stacking a second one makes
 * the D-pad focus indicator invisible, or the element unreachable, depending on ordering.
 * No-op outside TV (pass [enabled] = isTvUi()) so mobile/tablet layouts are unaffected.
 */
@Composable
fun Modifier.tvFocusable(
    interactionSource: InteractionSource,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusColor = MaterialTheme.colorScheme.primary
    return if (isFocused) this.border(TvFocusBorderWidth, focusColor, TvFocusBorderShape) else this
}

fun Modifier.selectedBackground(isSelected: Boolean): Modifier = if (isSelected) {
    composed {
        val alpha = if (isSystemInDarkTheme()) 0.16f else 0.22f
        val color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
        Modifier.drawBehind {
            drawRect(color)
        }
    }
} else {
    this
}

fun Modifier.secondaryItemAlpha(): Modifier = this.alpha(SECONDARY_ALPHA)

fun Modifier.clickableNoIndication(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) = this.combinedClickable(
    interactionSource = null,
    indication = null,
    onLongClick = onLongClick,
    onClick = onClick,
)

/**
 * For TextField, the provided [action] will be invoked when
 * physical enter key is pressed.
 *
 * Naturally, the TextField should be set to single line only.
 */
fun Modifier.runOnEnterKeyPressed(action: () -> Unit): Modifier = this.onPreviewKeyEvent {
    when (it.key) {
        Key.Enter, Key.NumPadEnter -> {
            action()
            true
        }
        else -> false
    }
}

/**
 * For TextField on AppBar, this modifier will request focus
 * to the element the first time it's composed.
 */
fun Modifier.showSoftKeyboard(show: Boolean): Modifier = if (show) {
    composed {
        val focusRequester = remember { FocusRequester() }
        var openKeyboard by rememberSaveable { mutableStateOf(show) }
        LaunchedEffect(focusRequester) {
            if (openKeyboard) {
                focusRequester.requestFocus()
                openKeyboard = false
            }
        }

        Modifier.focusRequester(focusRequester)
    }
} else {
    this
}

/**
 * For TextField, this modifier will clear focus when soft
 * keyboard is hidden.
 */
fun Modifier.clearFocusOnSoftKeyboardHide(
    onFocusCleared: (() -> Unit)? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardShowedSinceFocused by remember { mutableStateOf(false) }
    if (isFocused) {
        val imeVisible = WindowInsets.isImeVisible
        val focusManager = LocalFocusManager.current
        LaunchedEffect(imeVisible) {
            if (imeVisible) {
                keyboardShowedSinceFocused = true
            } else if (keyboardShowedSinceFocused) {
                focusManager.clearFocus()
                onFocusCleared?.invoke()
            }
        }
    }

    Modifier.onFocusChanged {
        if (isFocused != it.isFocused) {
            if (isFocused) {
                keyboardShowedSinceFocused = false
            }
            isFocused = it.isFocused
        }
    }
}
