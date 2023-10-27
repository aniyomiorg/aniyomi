package eu.kanade.tachiyomi.ui.player.settings.sheets

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PlayerChaptersItemBinding
import eu.kanade.tachiyomi.databinding.PlayerChaptersSheetBinding
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.widget.sheet.PlayerBottomSheetDialog
import `is`.xyz.mpv.MPVView.Chapter
import `is`.xyz.mpv.Utils
import kotlin.math.roundToInt

/** Sheet to show when Chapter selection buttons in player are clicked. */
class PlayerChaptersSheet(
    private val activity: PlayerActivity,
    @StringRes
    private val textRes: Int,
    private val seekToChapterMethod: (Chapter) -> Unit,
    private val chapters: List<Chapter>,
) : PlayerBottomSheetDialog(activity) {

    private lateinit var binding: PlayerChaptersSheetBinding
    private var wasPaused: Boolean? = null

    override fun createView(inflater: LayoutInflater): View {
        wasPaused = activity.player.paused
        activity.player.paused = true
        binding = PlayerChaptersSheetBinding.inflate(activity.layoutInflater, null, false)

        binding.chapterSelectionHeader.setText(textRes)
        chapters.forEachIndexed { i, chapter ->
            val chapterView = PlayerChaptersItemBinding.inflate(activity.layoutInflater).root
            chapterView.text = if (chapter.title.isNullOrBlank()) {
                Utils.prettyTime(chapter.time.roundToInt())
            } else {
                "${chapter.title} (${Utils.prettyTime(chapter.time.roundToInt())})"
            }
            // Highlighted the current chapter
            if (i == chapters.lastIndex) {
                if (activity.player.timePos!!.toInt() >= chapter.time.toInt()) {
                    chapterView.setBackgroundColor(context.getResourceColor(R.attr.colorPrimary))
                    chapterView.setTextColor(context.getResourceColor(R.attr.colorOnPrimary))
                }
            } else if (activity.player.timePos!!.toInt() >= chapter.time.toInt() &&
                activity.player.timePos!!.toInt() < chapters[i + 1].time.toInt()
            ) {
                chapterView.setBackgroundColor(context.getResourceColor(R.attr.colorPrimary))
                chapterView.setTextColor(context.getResourceColor(R.attr.colorOnPrimary))
            }
            chapterView.setOnClickListener {
                seekToChapterMethod(chapters[i])
                this.dismiss()
            }
            binding.linearLayout.addView(chapterView)
        }

        return binding.root
    }

    override fun dismiss() {
        activity.playerControls.showAndFadeControls()
        wasPaused?.let { activity.player.paused = it }
        super.dismiss()
    }
}
