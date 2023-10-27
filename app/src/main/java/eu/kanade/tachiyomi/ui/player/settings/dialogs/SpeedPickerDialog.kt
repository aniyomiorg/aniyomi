package eu.kanade.tachiyomi.ui.player.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SpeedPickerDialog(
    currentSpeed: Double,
    onSpeedChanged: (Float) -> Unit,
    onDismissRequest: () -> Unit,
) {
    fun Double.toHundredths(): BigDecimal {
        return BigDecimal(this).setScale(2, RoundingMode.FLOOR)
    }

    var speed by remember { mutableStateOf(currentSpeed.toHundredths()) }

    PlayerDialog(
        titleRes = R.string.title_speed_dialog,
        modifier = Modifier.fillMaxWidth(fraction = 0.8F),
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            Spacer(Modifier.height(8.dp))
            Text(
                text = speed.toString(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(4.dp))
            Slider(
                value = speed.toFloat(),
                onValueChange = { speed = it.toDouble().toHundredths() },
                valueRange = 0.2f..6f,
                onValueChangeFinished = { onSpeedChanged(speed.toFloat()) },
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
