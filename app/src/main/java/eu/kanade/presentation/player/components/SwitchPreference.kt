package eu.kanade.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun SwitchPreference(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .toggleable(value, true, Role.Switch, onValueChange)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        Switch(
            checked = value,
            onCheckedChange = null,
        )
    }
}
