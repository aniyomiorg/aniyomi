package eu.kanade.tachiyomi.ui.anime.episode

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.databinding.EpisodeDownloadViewBinding

class EpisodeDownloadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val binding: EpisodeDownloadViewBinding

    private var state = AnimeDownload.State.NOT_DOWNLOADED
    private var progress = 0

    private var downloadIconAnimator: ObjectAnimator? = null
    private var isAnimating = false

    init {
        binding = EpisodeDownloadViewBinding.inflate(LayoutInflater.from(context), this, false)
        addView(binding.root)
    }

    fun setState(state: AnimeDownload.State, progress: Int = 0) {
        val isDirty = this.state.value != state.value || this.progress != progress

        this.state = state
        this.progress = progress

        if (isDirty) {
            updateLayout()
        }
    }

    private fun updateLayout() {
        binding.downloadIconBorder.isVisible = state == AnimeDownload.State.NOT_DOWNLOADED

        binding.downloadIcon.isVisible = state == AnimeDownload.State.NOT_DOWNLOADED || state == AnimeDownload.State.DOWNLOADING
        if (state == AnimeDownload.State.DOWNLOADING) {
            if (!isAnimating) {
                downloadIconAnimator =
                    ObjectAnimator.ofFloat(binding.downloadIcon, "alpha", 1f, 0f).apply {
                        duration = 1000
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.REVERSE
                    }
                downloadIconAnimator?.start()
                isAnimating = true
            }
        } else {
            downloadIconAnimator?.cancel()
            binding.downloadIcon.alpha = 1f
            isAnimating = false
        }

        binding.downloadQueued.isVisible = state == AnimeDownload.State.QUEUE

        binding.downloadProgress.isVisible = state == AnimeDownload.State.DOWNLOADING ||
            (state == AnimeDownload.State.QUEUE && progress > 0)
        binding.downloadProgress.progress = progress

        binding.downloadedIcon.isVisible = state == AnimeDownload.State.DOWNLOADED

        binding.errorIcon.isVisible = state == AnimeDownload.State.ERROR
    }
}
