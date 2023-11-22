package eu.kanade.tachiyomi.ui.player.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.player.PlayerActivity

@Composable
fun PlayerControls(
    activity: PlayerActivity,
) {
    TopControls(activity)
}

@Composable
private fun TopControls(
    activity: PlayerActivity,
) {
    val viewModel = activity.viewModel
    val state by viewModel.state.collectAsState()

    Box(
        contentAlignment = Alignment.TopStart,
        modifier = Modifier.padding(top = 10.dp, start = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            @Suppress("DEPRECATION")
            IconButton(onClick = activity::onBackPressed) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = null,
                )
            }

            Column(Modifier.padding(horizontal = 10.dp)) {
                Text(
                    text = state.anime?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { viewModel.showEpisodeList() }
                )

                Text(
                    text = state.episode?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .alpha(0.5f)
                        .clickable { viewModel.showEpisodeList() },
                )
            }
        }
    }
}
