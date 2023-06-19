package eu.kanade.tachiyomi.ui.player.settings

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PrefSkipIntroLengthBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.SpeedPickerDialog
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

    // TODO: replace this with SliderPickerDialog
    internal fun speedPickerDialog(restoreState: StateRestoreCallback) {
        val picker = SpeedPickerDialog()
        with(HideBarsMaterialAlertDialogBuilder(activity)) {
            setTitle(R.string.title_speed_dialog)
            setView(picker.buildView(LayoutInflater.from(context)))
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                picker.number?.let {
                    activity.playerPreferences.playerSpeed().set(it.toFloat())
                    if (picker.isInteger()) {
                        MPVLib.setPropertyInt("speed", it.toInt())
                    } else {
                        MPVLib.setPropertyDouble("speed", it)
                    }
                }
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restoreState() }
            create()
            show()
        }
        picker.number = MPVLib.getPropertyDouble("speed")
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

    internal fun episodeListDialog(restoreState: StateRestoreCallback) {
    }
}
