package eu.kanade.tachiyomi.ui.watcher.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.WatcherTransitionViewBinding
import eu.kanade.tachiyomi.ui.watcher.model.EpisodeTransition

class WatcherTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val binding: WatcherTransitionViewBinding

    init {
        binding = WatcherTransitionViewBinding.inflate(LayoutInflater.from(context), this, true)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(transition: EpisodeTransition) {
        when (transition) {
            is EpisodeTransition.Prev -> bindPrevEpisodeTransition(transition)
            is EpisodeTransition.Next -> bindNextEpisodeTransition(transition)
        }

        missingEpisodeWarning(transition)
    }

    /**
     * Binds a previous episode transition on this view and subscribes to the page load status.
     */
    private fun bindPrevEpisodeTransition(transition: EpisodeTransition) {
        val prevEpisode = transition.to

        val hasPrevEpisode = prevEpisode != null
        binding.lowerText.isVisible = hasPrevEpisode
        if (hasPrevEpisode) {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_TEXT_START
            binding.upperText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_previous)) }
                append("\n${prevEpisode!!.episode.name}")
            }
            binding.lowerText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_current)) }
                append("\n${transition.from.episode.name}")
            }
        } else {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_CENTER
            binding.upperText.text = context.getString(R.string.transition_no_previous)
        }
    }

    /**
     * Binds a next episode transition on this view and subscribes to the load status.
     */
    private fun bindNextEpisodeTransition(transition: EpisodeTransition) {
        val nextEpisode = transition.to

        val hasNextEpisode = nextEpisode != null
        binding.lowerText.isVisible = hasNextEpisode
        if (hasNextEpisode) {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_TEXT_START
            binding.upperText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_finished)) }
                append("\n${transition.from.episode.name}")
            }
            binding.lowerText.text = buildSpannedString {
                bold { append(context.getString(R.string.transition_next)) }
                append("\n${nextEpisode!!.episode.name}")
            }
        } else {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_CENTER
            binding.upperText.text = context.getString(R.string.transition_no_next)
        }
    }

    private fun missingEpisodeWarning(transition: EpisodeTransition) {
        if (transition.to == null) {
            binding.warning.isVisible = false
            return
        }

        val hasMissingEpisodes = when (transition) {
            is EpisodeTransition.Prev -> hasMissingEpisodes(transition.from, transition.to)
            is EpisodeTransition.Next -> hasMissingEpisodes(transition.to, transition.from)
        }

        if (!hasMissingEpisodes) {
            binding.warning.isVisible = false
            return
        }

        val episodeDifference = when (transition) {
            is EpisodeTransition.Prev -> calculateEpisodeDifference(transition.from, transition.to)
            is EpisodeTransition.Next -> calculateEpisodeDifference(transition.to, transition.from)
        }

        binding.warningText.text = resources.getQuantityString(R.plurals.missing_chapters_warning, episodeDifference.toInt(), episodeDifference.toInt())
        binding.warning.isVisible = true
    }
}
