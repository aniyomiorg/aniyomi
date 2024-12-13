package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource

@Composable
fun DoubleTapSeekSecondsView(
    modifier: Modifier = Modifier,
    seconds: Int,
) {
    val animationDuration = 750L

    val alpha1 = remember { Animatable(0f) }
    val alpha2 = remember { Animatable(0f) }
    val alpha3 = remember { Animatable(0f) }

    LaunchedEffect(animationDuration) {
        while (true) {
            alpha1.animateTo(1f, animationSpec = tween((animationDuration / 5).toInt()))
            alpha2.animateTo(1f, animationSpec = tween((animationDuration / 5).toInt()))
            alpha3.animateTo(1f, animationSpec = tween((animationDuration / 5).toInt()))
            alpha1.animateTo(0f, animationSpec = tween((animationDuration / 5).toInt()))
            alpha2.animateTo(0f, animationSpec = tween((animationDuration / 5).toInt()))
            alpha3.animateTo(0f, animationSpec = tween((animationDuration / 5).toInt()))
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rotation = if (seconds > 0) 0f else 180f
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.rotate(rotation)
        ) {
            DoubleTapArrow(alpha1.value)
            DoubleTapArrow(alpha2.value)
            DoubleTapArrow(alpha3.value)
        }

        Text(
            text = pluralStringResource(MR.plurals.seconds, seconds, seconds),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = Color.White
        )
    }
}

@Composable
private fun DoubleTapArrow(
    alpha: Float,
) {
    Icon(
        painter = painterResource(R.drawable.ic_play_seek_triangle),
        contentDescription = null,
        modifier = Modifier
            .size(width = 16.dp, height = 20.dp)
            .alpha(alpha = alpha),
        tint = Color.White
    )
}
