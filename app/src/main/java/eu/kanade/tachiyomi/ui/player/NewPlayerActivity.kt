package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
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
import `is`.xyz.mpv.Utils
import logcat.LogPriority
import nucleus.factory.RequiresPresenter
import uy.kohesive.injekt.injectLazy
import kotlin.math.abs

@RequiresPresenter(NewPlayerPresenter::class)
class NewPlayerActivity : BaseRxActivity<NewPlayerActivityBinding, NewPlayerPresenter>(), MPVLib.EventObserver {

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

    private var userIsOperatingSeekbar = false

    // private var lockedUI = false

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) {
                return
            }
            player.timePos = progress
            updatePlaybackPos(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = false
        }
    }

    private var currentVideoList: List<Video>? = null

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        logcat { "bruh" }
        applyAppTheme(preferences)
        super.onCreate(savedInstanceState)

        binding = NewPlayerActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setVisibilities()
        player.initialize(applicationContext.filesDir.path)
        player.addObserver(this)

        binding.playbackSeekbar.setOnSeekBarChangeListener(seekBarChangeListener)
        // player.playFile(currentVideoList!!.first().videoUrl!!.toString())

        binding.nextBtn.setOnClickListener { presenter.nextEpisode() }
        binding.prevBtn.setOnClickListener { presenter.previousEpisode() }

        if (presenter?.needsInit() == true) {
            val anime = intent.extras!!.getLong("anime", -1)
            val episode = intent.extras!!.getLong("episode", -1)
            if (anime == -1L || episode == -1L) {
                finish()
                return
            }
            presenter.init(anime, episode)
        }
    }

    @Suppress("DEPRECATION")
    private fun setVisibilities() {
        // TODO: replace this atrocity
        binding.root.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    fun updatePlaybackPos(position: Int) {
        binding.playbackPositionTxt.text = prettyTime(position)
        if (!userIsOperatingSeekbar) {
            binding.playbackSeekbar.progress = position
        }

        updateDecoderButton()
        updateSpeedButton()
    }

    private fun updatePlaybackDuration(duration: Int) {
        binding.playbackDurationTxt.text = Utils.prettyTime(duration)
        if (!userIsOperatingSeekbar) {
            binding.playbackSeekbar.max = duration
        }
    }

    private fun updateDecoderButton() {
        if (binding.cycleDecoderBtn.visibility != View.VISIBLE) {
            return
        }
        binding.cycleDecoderBtn.text = if (player.hwdecActive) "HW" else "SW"
    }

    private fun updateSpeedButton() {
        binding.cycleSpeedBtn.text = getString(R.string.ui_speed, player.playbackSpeed)
    }

    @Suppress("UNUSED_PARAMETER")
    fun playPause(view: View) = player.cyclePause()

    data class TrackData(val track_id: Int, val track_type: String)
    private fun trackSwitchNotification(f: () -> TrackData) {
        val (track_id, track_type) = f()
        val trackPrefix = when (track_type) {
            "audio" -> getString(R.string.track_audio)
            "sub" -> getString(R.string.track_subs)
            "video" -> "Video"
            else -> "???"
        }

        if (track_id == -1) {
            toast("$trackPrefix ${getString(R.string.track_off)}")
            return
        }

        val trackName = player.tracks[track_type]?.firstOrNull { it.mpvId == track_id }?.name ?: "???"
        toast("$trackPrefix $trackName")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleAudio(view: View) = trackSwitchNotification {
        player.cycleAudio(); TrackData(player.aid, "audio")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSub(view: View) = trackSwitchNotification {
        player.cycleSub(); TrackData(player.sid, "sub")
    }

    @Suppress("UNUSED_PARAMETER")
    fun switchDecoder(view: View) {
        player.cycleHwdec()
        updateDecoderButton()
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSpeed(view: View) {
        player.cycleSpeed()
        updateSpeedButton()
    }

    private fun refreshUi() {
        // forces update of entire UI, used when resuming the activity
        val paused = player.paused ?: return
        updatePlaybackStatus(paused)
        player.timePos?.let { updatePlaybackPos(it) }
        player.duration?.let { updatePlaybackDuration(it) }
        updatePlaylistButtons()
        player.loadTracks()
    }

    private fun updatePlaylistButtons() {
        val plCount = presenter.episodeList.size
        val plPos = presenter.getCurrentEpisodeIndex()

        if (plCount == 1) {
            // use View.GONE so the buttons won't take up any space
            binding.prevBtn.visibility = View.GONE
            binding.nextBtn.visibility = View.GONE
            return
        }
        binding.prevBtn.visibility = View.VISIBLE
        binding.nextBtn.visibility = View.VISIBLE

        val g = ContextCompat.getColor(this, R.color.tint_disabled)
        val w = ContextCompat.getColor(this, R.color.tint_normal)
        binding.prevBtn.imageTintList = ColorStateList.valueOf(if (plPos == 0) g else w)
        binding.nextBtn.imageTintList = ColorStateList.valueOf(if (plPos == plCount - 1) g else w)
    }

    private fun updatePlaybackStatus(paused: Boolean) {
        val r = if (paused) R.drawable.ic_play_arrow_black_24dp else R.drawable.ic_pause_black_24dp
        binding.playBtn.setImageResource(r)

        if (paused) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        currentVideoList?.first()?.videoUrl.let {
            MPVLib.command(arrayOf("loadfile", it))
            // presenter.currentEpisode?.last_second_seen?.let { pos -> player.timePos = (pos / 1000L).toInt() }
        }
        refreshUi()
        updatePlaylistButtons()
    }

    fun setHttpHeaders(headers: Map<String, String>) {
        val httpHeaderString = headers.map {
            it.key + ": " + it.value
        }.joinToString(",")
        MPVLib.setOptionString("http-header-fields", httpHeaderString)
        MPVLib.setOptionString("tls-verify", "no")
    }

    private fun prettyTime(d: Int, sign: Boolean = false): String {
        if (sign) {
            return (if (d >= 0) "+" else "-") + prettyTime(abs(d))
        }

        val hours = d / 3600
        val minutes = d % 3600 / 60
        val seconds = d % 60
        if (hours == 0) {
            return "%02d:%02d".format(minutes, seconds)
        }
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    // mpv events

    private fun eventPropertyUi(property: String, value: Long) {
        when (property) {
            "time-pos" -> updatePlaybackPos(value.toInt())
            "duration" -> updatePlaybackDuration(value.toInt())
        }
    }

    private fun eventPropertyUi(property: String, value: Boolean) {
        when (property) {
            "pause" -> updatePlaybackStatus(value)
        }
    }

    override fun eventProperty(property: String) {}

    override fun eventProperty(property: String, value: Boolean) {
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: Long) {
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: String) {}

    override fun event(eventId: Int) {}
}
