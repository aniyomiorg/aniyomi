package eu.kanade.tachiyomi.ui.player.settings.sheets.subtitle

import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yubyf.truetypeparser.TTFFile
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.player.settings.PlayerSettingsScreenModel
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.core.common.storage.extension
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun SubtitleSettingsSheet(
    screenModel: PlayerSettingsScreenModel,
    onDismissRequest: () -> Unit,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = persistentListOf(
            stringResource(MR.strings.player_subtitle_settings_filters),
            stringResource(MR.strings.player_subtitle_settings_delay_tab),
            stringResource(MR.strings.player_subtitle_settings_font_tab),
            stringResource(MR.strings.player_subtitle_settings_color_tab),
        ),
        hideSystemBars = true,
    ) { page ->
        Column(
            modifier = Modifier
                .padding(top = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> FiltersPage(screenModel)
                1 -> StreamsDelayPage(screenModel)
                2 -> SubtitleFontPage(screenModel)
                3 -> SubtitleColorPage(screenModel)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun OutLineText(
    text: String,
    font: Typeface,
    outlineColor: Color = Color.Black,
    textColor: Color = Color.White,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    backgroundColor: Color = Color.Black,
) {
    val textPaintStroke = Paint().asFrameworkPaint().apply {
        typeface = font
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        textSize = 48f
        color = outlineColor.toArgb()
        strokeWidth = 12f
        strokeMiter = 8f
        strokeJoin = android.graphics.Paint.Join.ROUND
        // change the text alignment from left to center (basically shift the anchor point of the text)
        // keep in mind that this only affects horizontal alignment
        // https://developer.android.com/reference/android/graphics/Paint.Align
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = isBold
        textSkewX = if (isItalic) -0.25f else 0f
    }
    val textPaint = Paint().asFrameworkPaint().apply {
        typeface = font
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
        textSize = 48f
        color = textColor.toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = isBold
        textSkewX = if (isItalic) -0.25f else 0f
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas {
            it.nativeCanvas.drawRect(
                Rect(
                    0,
                    size.height.toInt(),
                    size.width.toInt(),
                    0,
                ),
                Paint().asFrameworkPaint().apply {
                    style = android.graphics.Paint.Style.FILL
                    color = backgroundColor.toArgb()
                },
            )
            // Considering that the canvas's top left corner is at (0,0),
            // position the text at the center of the canvas, which is at (width/2),
            // and place it in the third quarter of the canvas, aligning it with the top.
            // Essentially, it will be at the bottom center.
            // It's approximately centered, I guess.
            it.nativeCanvas.drawText(
                text,
                size.width / 2,
                (size.height * 3) / 4,
                textPaintStroke,
            )
            it.nativeCanvas.drawText(
                text,
                size.width / 2,
                (size.height * 3) / 4,
                textPaint,
            )
        }
    }
}

@Composable
fun SubtitlePreview(
    font: String,
    isBold: Boolean,
    isItalic: Boolean,
    textColor: Color,
    borderColor: Color,
    backgroundColor: Color,
) {
    val storageManager: StorageManager = Injekt.get()
    val tempFileManager: UniFileTempFileManager = UniFileTempFileManager(LocalContext.current)
    val fontsDir = storageManager.getFontsDirectory()
    val fontMap = fontsDir?.listFiles()?.filter { file ->
        file.extension!!.equals("ttf", true) ||
            file.extension!!.equals("otf", true)
    }?.associate {
        TTFFile.open(it.openInputStream()).families.values.toTypedArray()[0] to it
    } ?: emptyMap()

    val fontFile = fontMap.keys.firstOrNull { it.contains(font, true) }
        ?.let {
            Typeface.createFromFile(fontMap[it]?.let { fontFile -> tempFileManager.createTempFile(fontFile) })
        } ?: Typeface.createFromAsset(LocalContext.current.assets, "subfont.ttf")

    Box(
        modifier = Modifier
            .padding(vertical = MaterialTheme.padding.medium)
            .height(32.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.8f).background(color = backgroundColor)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                OutLineText(
                    text = stringResource(MR.strings.player_subtitle_settings_example),
                    font = fontFile,
                    outlineColor = borderColor,
                    textColor = textColor,
                    isBold = isBold,
                    isItalic = isItalic,
                    backgroundColor = backgroundColor,
                )
            } else {
                Text(
                    text = stringResource(MR.strings.player_subtitle_settings_example),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        shadow = Shadow(color = borderColor, blurRadius = 7.5f),
                        color = textColor,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}
