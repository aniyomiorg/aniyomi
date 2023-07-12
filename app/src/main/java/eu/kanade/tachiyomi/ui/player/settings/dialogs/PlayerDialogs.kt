package eu.kanade.tachiyomi.ui.player.settings.dialogs

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PrefSkipIntroLengthBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import `is`.xyz.mpv.StateRestoreCallback

class PlayerDialogs(val activity: PlayerActivity) {

    /**
     * Class to override [MaterialAlertDialogBuilder] to hide the navigation and status bars
     */
    private inner class HideBarsMaterialAlertDialogBuilder(context: Context) : MaterialAlertDialogBuilder(context) {
        override fun create(): AlertDialog {
            return super.create().apply {
                val window = this.window ?: return@apply
                val alertWindowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                alertWindowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                alertWindowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        }

        override fun show(): AlertDialog {
            return super.show().apply {
                val window = this.window ?: return@apply
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        }
    }

    internal fun decoderDialog(restoreState: StateRestoreCallback) {
        val items = mutableListOf(
            Pair("${HwDecState.HW.title} (${HwDecState.HW.mpvValue})", HwDecState.HW.mpvValue),
            Pair(HwDecState.SW.title, HwDecState.SW.mpvValue),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            items.add(
                index = 0,
                Pair("${HwDecState.HW_PLUS.title} (${HwDecState.HW_PLUS.mpvValue})", HwDecState.HW_PLUS.mpvValue),
            )
        }

        var hwdecActive = activity.playerPreferences.standardHwDec().get()
        val selectedIndex = items.indexOfFirst { it.second == hwdecActive }
        with(HideBarsMaterialAlertDialogBuilder(activity)) {
            setTitle(R.string.player_hwdec_dialog_title)
            setSingleChoiceItems(items.map { it.first }.toTypedArray(), selectedIndex) { dialog, idx ->
                hwdecActive = items[idx].second
                activity.playerPreferences.standardHwDec().set(hwdecActive)
                activity.mpvUpdateHwDec(HwDecState.get(hwdecActive))
                dialog.dismiss()
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restoreState() }
            create()
            show()
        }
    }

    internal fun skipIntroDialog(restoreState: StateRestoreCallback) {
        var newSkipIntroLength = activity.viewModel.getAnimeSkipIntroLength()

        with(HideBarsMaterialAlertDialogBuilder(activity)) {
            setTitle(R.string.pref_intro_length)
            val binding = PrefSkipIntroLengthBinding.inflate(LayoutInflater.from(activity))

            with(binding.skipIntroColumn) {
                value = activity.viewModel.getAnimeSkipIntroLength()
                setOnValueChangedListener { _, _, newValue ->
                    newSkipIntroLength = newValue
                }
            }

            setView(binding.root)
            setNeutralButton(R.string.label_default) { _, _ ->
                activity.viewModel.setAnimeSkipIntroLength(activity.playerPreferences.defaultIntroLength().get())
            }
            setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                when (newSkipIntroLength) {
                    0 -> activity.viewModel.setAnimeSkipIntroLength(activity.playerPreferences.defaultIntroLength().get())
                    activity.viewModel.getAnimeSkipIntroLength() -> dialog.cancel()
                    else -> activity.viewModel.setAnimeSkipIntroLength(newSkipIntroLength)
                }
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restoreState() }
            create()
            show()
        }
    }
}

@Composable
fun PlayerDialog(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(fraction = 0.8F),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            val systemUIController = rememberSystemUiController()
            systemUIController.isSystemBarsVisible = false
            systemUIController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                content()
            }
        }
    }
}
