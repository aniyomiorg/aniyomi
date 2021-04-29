package eu.kanade.tachiyomi.ui.watcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode

const val STATE_RESUME_WINDOW = "resumeWindow"
const val STATE_RESUME_POSITION = "resumePosition"
const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
const val STATE_PLAYER_PLAYING = "playerOnPlay"

class WatcherActivity : AppCompatActivity() {

    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var playerView: PlayerView

    private var duration: Long = 0
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isFullscreen = false
    private var isPlayerPlaying = true
    private var mediaItem = MediaItem.Builder()
        .setUri("bruh")
        .setMimeType(MimeTypes.VIDEO_MP4)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.watcher_activity)
        playerView = findViewById(R.id.player_view)
        dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "xyz.jmir.tachiyomi.mi"))
        mediaItem = MediaItem.Builder()
            .setUri("https://ecchi.ninja/uploads/AnimePahe_Vivy_-_Fluorite_Eyes_Song_-_01_1080p_SubsPlease_wslP.mp4")
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
        playerView.player = exoPlayer
        duration = exoPlayer.duration
    }

    override fun onBackPressed() {
        releasePlayer()
        super.onBackPressed()
    }

    private fun releasePlayer() {
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        val episode = intent.getSerializableExtra("episode") as Episode
        val returnIntent = intent
        Log.i("bruhh", exoPlayer.duration.toString())
        returnIntent.putExtra("seconds_result", playbackPosition)
        returnIntent.putExtra("total_seconds_result", exoPlayer.duration)
        returnIntent.putExtra("episode", episode)
        setResult(RESULT_OK, returnIntent)
        exoPlayer.release()
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

    companion object {
        fun newIntent(context: Context, anime: Anime, episode: Episode): Intent {
            return Intent(context, WatcherActivity::class.java).apply {
                putExtra("anime", anime.id)
                putExtra("episode", episode)
                putExtra("second", episode.last_second_seen)
                putExtra("uri", episode.url)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
}
