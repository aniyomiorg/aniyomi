package eu.kanade.presentation.player.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import tachiyomi.presentation.core.components.material.padding

@SuppressLint("UnrememberedMutableState")
@Composable
fun ExpandableCard(
    isExpanded: Boolean,
    title: @Composable (Boolean) -> Unit,
    onExpand: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    shape: Shape = CardDefaults.shape,
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable () -> Unit,
) {
    val rotationState by animateFloatAsState(if (isExpanded) 0f else 180f, label = "card_rotation")
    Card(
        modifier = modifier.animateContentSize(),
        colors = colors,
        shape = shape,
        border = border,
        elevation = elevation,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = { onExpand(!isExpanded) })
                .padding(start = MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            title(isExpanded)
            Spacer(Modifier.weight(1f))
            IconButton(
                modifier = Modifier.rotate(rotationState),
                onClick = { onExpand(!isExpanded) },
            ) {
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        Box(Modifier.animateContentSize()) {
            if (isExpanded) content()
        }
    }
}

@Composable
@Preview
private fun PreviewExpandableCard() {
    var isExpanded by remember { mutableStateOf(true) }

    ExpandableCard(
        isExpanded,
        title = { Text("Hello World") },
        content = { Text("SPOjao;sjd") },
        onExpand = { isExpanded = it },
    )
}
