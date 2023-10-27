package eu.kanade.tachiyomi.ui.player.settings.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.material.TextButton

// TODO: (Merge_Change) stringResource "android.R.string.ok" to be replaced with
//  "R.string.action_ok"

@Composable
fun PlayerDialog(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    hideSystemBars: Boolean = true,
    onConfirmRequest: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    content: @Composable (() -> Unit)? = null,
) {
    val onConfirm = {
        onConfirmRequest?.invoke()
        onDismissRequest()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
            if (hideSystemBars) {
                rememberSystemUiController().apply {
                    isSystemBarsVisible = false
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                content?.invoke()

                if (onConfirmRequest != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(R.string.action_cancel))
                        }

                        TextButton(onClick = onConfirm) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            }
        }
    }
}
