package eu.kanade.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

@Composable
fun FloatingActionAddButton(
    lazyListState: LazyListState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        text = { Text(text = stringResource(MR.strings.action_add)) },
        icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
        onClick = onClick,
        expanded = lazyListState.shouldExpandFAB(),
        modifier = modifier,
    )
}
