package eu.kanade.tachiyomi.ui.watcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.vkay94.dtpv.DoubleTapPlayerView
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
import eu.kanade.tachiyomi.util.view.hideBar
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.*

const val STATE_RESUME_WINDOW = "resumeWindow"
const val STATE_RESUME_POSITION = "resumePosition"
const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
const val STATE_PLAYER_PLAYING = "playerOnPlay"

class WatcherActivity : AppCompatActivity() {

    private val preferences: PreferencesHelper = Injekt.get()
    private val incognitoMode = preferences.incognitoMode().get()
    private val db: AnimeDatabaseHelper = Injekt.get()
    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var playerView: DoubleTapPlayerView
    private lateinit var youTubeDoubleTap: YouTubeOverlay
    private lateinit var skipBtn: TextView
    private lateinit var nextBtn: ImageButton
    private lateinit var prevBtn: ImageButton

    private var duration: Long = 0
    private var currentWindow = 0
    private var playbackPosition = 0L
    private var isFullscreen = false
    private var isPlayerPlaying = true
    private var mediaItem = MediaItem.Builder()
        .setUri("bruh")
        .setMimeType(MimeTypes.VIDEO_MP4)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.watcher_activity)
        window.decorView.setOnSystemUiVisibilityChangeListener {
            window.hideBar()
        }
        playerView = findViewById(R.id.player_view)
        youTubeDoubleTap = findViewById(R.id.youtube_overlay)
        youTubeDoubleTap
            .performListener(object : YouTubeOverlay.PerformListener {
                override fun onAnimationStart() {
                    // Do UI changes when circle scaling animation starts (e.g. hide controller views)
                    youTubeDoubleTap.visibility = View.VISIBLE
                }

                override fun onAnimationEnd() {
                    // Do UI changes when circle scaling animation starts (e.g. show controller views)
                    youTubeDoubleTap.visibility = View.GONE
                }
            })
        skipBtn = findViewById(R.id.watcher_controls_skip_btn)
        nextBtn = findViewById(R.id.watcher_controls_next)
        prevBtn = findViewById(R.id.watcher_controls_prev)

        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "xyz.jmir.tachiyomi.mi"))
        mediaItem = MediaItem.Builder()
            .setUri(intent.getStringExtra("uri"))
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
        playbackPosition = intent.extras!!.getLong("second")

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            playbackPosition = intent.extras!!.getLong("second")
            isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
            isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
        }
    }

    private fun initPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build().apply {
            playWhenReady = isPlayerPlaying
            seekTo(currentWindow, playbackPosition)
            setMediaItem(mediaItem, false)
            prepare()
        }
        class PlayerEventListener : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                playerView.keepScreenOn = !(
                    playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED ||
                        !playWhenReady
                    )
            }
            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}
            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onPlayerError(error: ExoPlaybackException) {}
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
        }
        exoPlayer.addListener(PlayerEventListener())
        skipBtn.setOnClickListener { exoPlayer.seekTo(exoPlayer.currentPosition + 85000) }
        if (intent.getBooleanExtra("hasNextEpisode", false)) {
            nextBtn.setOnClickListener {
                nextEpisode()
            }
        }
        if (intent.getBooleanExtra("hasPreviousEpisode", false)) {
            prevBtn.setOnClickListener {
                previousEpisode()
            }
        }
        youTubeDoubleTap.player(exoPlayer)
        playerView.player = exoPlayer
        duration = exoPlayer.duration
    }

    override fun onBackPressed() {
        releasePlayer()
        super.onBackPressed()
    }

    private fun releasePlayer() {
        youTubeDoubleTap.player(exoPlayer)
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        val episode = intent.getSerializableExtra("episode") as Episode
        val anime = intent.getSerializableExtra("anime_anime") as Anime
        saveEpisodeHistory(EpisodeItem(episode, anime))
        val returnIntent = intent
        returnIntent.putExtra("seconds_result", playbackPosition)
        returnIntent.putExtra("total_seconds_result", exoPlayer.duration)
        returnIntent.putExtra("episode", episode)
        setResult(RESULT_OK, returnIntent)
        exoPlayer.release()
        super.onBackPressed()
    }

    private fun nextEpisode() {
        youTubeDoubleTap.player(exoPlayer)
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        val episode = intent.getSerializableExtra("episode") as Episode
        val returnIntent = intent
        returnIntent.putExtra("seconds_result", playbackPosition)
        returnIntent.putExtra("total_seconds_result", exoPlayer.duration)
        returnIntent.putExtra("episode", episode)
        returnIntent.putExtra("nextResult", true)
        setResult(RESULT_OK, returnIntent)
        exoPlayer.release()
        super.onBackPressed()
    }

    private fun previousEpisode() {
        youTubeDoubleTap.player(exoPlayer)
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        val episode = intent.getSerializableExtra("episode") as Episode
        val returnIntent = intent
        returnIntent.putExtra("seconds_result", playbackPosition)
        returnIntent.putExtra("total_seconds_result", exoPlayer.duration)
        returnIntent.putExtra("episode", episode)
        returnIntent.putExtra("previousResult", true)
        setResult(RESULT_OK, returnIntent)
        exoPlayer.release()
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RESUME_WINDOW, exoPlayer.currentWindowIndex)
        outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            playerView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23) {
            initPlayer()
            playerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    private fun saveEpisodeHistory(episode: EpisodeItem) {
        if (!incognitoMode) {
            val history = AnimeHistory.create(episode.episode).apply { last_seen = Date().time }
            db.updateAnimeHistoryLastSeen(history).asRxCompletable()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    companion object {
        fun newIntent(context: Context, anime: Anime, episode: Episode, episodeList: List<EpisodeItem>, url: String): Intent {
            return Intent(context, WatcherActivity::class.java).apply {
                putExtra("anime", anime.id)
                putExtra("anime_anime", anime)
                putExtra("episode", episode)
                putExtra("second", episode.last_second_seen)
                putExtra("uri", url)
                if (episodeList.isNotEmpty()) {
                    putExtra("hasNextEpisode", episode.episode_number < episodeList[0].episode_number)
                    putExtra("hasPreviousEpisode", episode.episode_number > episodeList[episodeList.size - 1].episode_number)
                } else {
                    putExtra("hasNextEpisode", false)
                    putExtra("hasPreviousEpisode", false)
                }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
}
