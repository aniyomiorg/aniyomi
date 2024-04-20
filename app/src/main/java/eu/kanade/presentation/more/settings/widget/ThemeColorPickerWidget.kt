package eu.kanade.presentation.more.settings.widget

import android.graphics.PointF
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker

@Composable
internal fun ThemeColorPickerWidget(
    value: PointF,
    controller: ColorPickerController,
    onItemClick: (Int) -> Unit,
) {
    BasePreferenceWidget(
        subcomponent = {
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .padding(10.dp),
                controller = controller,
                onColorChanged = { colorEnvelope: ColorEnvelope ->
                    val color: Int = colorEnvelope.color.toArgb()
                    onItemClick(color)
                }
            )
        }
    )
}
