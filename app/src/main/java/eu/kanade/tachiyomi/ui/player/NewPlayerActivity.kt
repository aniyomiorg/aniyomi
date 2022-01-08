package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.NewPlayerActivityBinding
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity.Companion.applyAppTheme
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import `is`.xyz.mpv.MPVLib
import logcat.LogPriority
import nucleus.factory.RequiresPresenter
import uy.kohesive.injekt.injectLazy

@RequiresPresenter(NewPlayerPresenter::class)
class NewPlayerActivity : BaseRxActivity<NewPlayerActivityBinding, NewPlayerPresenter>() {

    companion object {
        fun newIntent(context: Context, anime: Anime, episode: Episode): Intent {
            return Intent(context, NewPlayerActivity::class.java).apply {
                putExtra("anime", anime.id)
                putExtra("episode", episode.id)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    private val player get() = binding.player

    var currentVideoList: List<Video>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        logcat { "bruh" }
        applyAppTheme(preferences)
        super.onCreate(savedInstanceState)

        binding = NewPlayerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (presenter?.needsInit() == true) {
            val anime = intent.extras!!.getLong("anime", -1)
            val episode = intent.extras!!.getLong("episode", -1)
            if (anime == -1L || episode == -1L) {
                finish()
                return
            }
            presenter.init(anime, episode)
        }

        binding.button.setOnClickListener {
            Log.i("bruh", currentVideoList!!.first().uri!!.toString())
            player.playFile(currentVideoList!!.first().uri!!.toString())
            MPVLib.command(arrayOf("loadfile", currentVideoList!!.first().uri!!.toString()))
        }

        binding.button2.setOnClickListener {
            player.initialize(applicationContext.filesDir.path)
        }
    }

    override fun onDestroy() {
        player.destroy()
        super.onDestroy()
    }

    /**
     * Called from the presenter if the initial load couldn't load the videos of the episode. In
     * this case the activity is closed and a toast is shown to the user.
     */
    fun setInitialEpisodeError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
        finish()
        toast(error.message)
    }

    fun setVideoList(videos: List<Video>) {
        logcat(LogPriority.INFO) { "loaded!!" }
        currentVideoList = videos
    }
}
