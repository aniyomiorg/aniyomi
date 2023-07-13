package eu.kanade.tachiyomi.ui.player.settings.sheets

import android.os.Bundle
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.widget.sheet.TabbedPlayerBottomSheetDialog

class PlayerSettingsSheet(
    private val activity: PlayerActivity,
) : TabbedPlayerBottomSheetDialog(activity) {

    private var wasPaused: Boolean? = null

    private val videoQualitySettings = activity.qualityTracksTab(this::dismiss) as View
    private val subtitleSettings = activity.subtitleTracksTab(this::dismiss) as View
    private val audioSettings = activity.audioTracksTab(this::dismiss) as View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wasPaused = activity.player.paused
        activity.player.paused = true

        behavior.isFitToContents = false
        behavior.halfExpandedRatio = 0.15f
    }

    override fun getTabs(): List<Pair<View, Int>> {
        val tabs = mutableListOf(
            Pair(subtitleSettings, R.string.subtitle_dialog_header),
            Pair(audioSettings, R.string.audio_dialog_header),
        )
        if (activity.viewModel.isEpisodeOnline() == true) {
            tabs.add(0, Pair(videoQualitySettings, R.string.quality_dialog_header))
        }
        return tabs.toList()
    }

    override fun dismiss() {
        activity.playerControls.showAndFadeControls()
        wasPaused?.let { activity.player.paused = it }
        super.dismiss()
        activity.refreshUi()
    }
}
