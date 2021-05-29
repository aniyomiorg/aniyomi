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
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.view.hideBar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

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

    private lateinit var episode: Episode
    private lateinit var anime: Anime
    private lateinit var source: AnimeSource
    private lateinit var uri: String
    private lateinit var episodeList: ArrayList<Episode>

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
        anime = intent.getSerializableExtra("anime") as Anime
        episode = intent.getSerializableExtra("episode") as Episode
        source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
        episodeList = intent.getSerializableExtra("episodeList") as ArrayList<Episode>
        uri = EpisodeLoader.getUri(episode, anime, source)
        mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
        playbackPosition = episode.last_second_seen

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
        if (episodeList.indexOf(episode) != episodeList.lastIndex && episodeList.isNotEmpty()) {
            nextBtn.setOnClickListener {
                nextEpisode()
            }
        }
        if (episodeList.indexOf(episode) != 0 && episodeList.isNotEmpty()) {
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
        saveEpisodeHistory(EpisodeItem(episode, anime))
        setEpisodeProgress(episode, anime, exoPlayer.currentPosition, exoPlayer.duration)
        exoPlayer.release()
    }

    private fun nextEpisode() {
        saveEpisodeHistory(EpisodeItem(episode, anime))
        setEpisodeProgress(episode, anime, exoPlayer.currentPosition, exoPlayer.duration)
        episode = episodeList[episodeList.indexOf(episode) + 1]
        uri = EpisodeLoader.getUri(episode, anime, source)
        mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
        exoPlayer.setMediaItem(mediaItem, episode.last_second_seen)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun previousEpisode() {
        saveEpisodeHistory(EpisodeItem(episode, anime))
        setEpisodeProgress(episode, anime, exoPlayer.currentPosition, exoPlayer.duration)
        episode = episodeList[episodeList.indexOf(episode) - 1]
        uri = EpisodeLoader.getUri(episode, anime, source)
        mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
        exoPlayer.setMediaItem(mediaItem, episode.last_second_seen)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
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

    private fun setEpisodeProgress(episode: Episode, anime: Anime, seconds: Long, totalSeconds: Long) {
        if (!incognitoMode) {
            if (totalSeconds > 0L) {
                episode.last_second_seen = seconds
                episode.total_seconds = totalSeconds
                if (!episode.seen) episode.seen = episode.last_second_seen > episode.total_seconds * 0.85
                val episodes = listOf(EpisodeItem(episode, anime))
                launchIO {
                    db.updateEpisodesProgress(episodes).executeAsBlocking()
                    if (preferences.autoUpdateTrack() && episode.seen) {
                        updateTrackEpisodeSeen(episode)
                    }
                    if (preferences.removeAfterMarkedAsRead()) {
                        launchIO {
                            try {
                                val downloadManager: AnimeDownloadManager = Injekt.get()
                                val source: AnimeSource = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
                                downloadManager.deleteEpisodes(episodes, anime, source).forEach {
                                    if (it is EpisodeItem) {
                                        it.status = AnimeDownload.State.NOT_DOWNLOADED
                                        it.download = null
                                    }
                                }
                            } catch (e: Throwable) {
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateTrackEpisodeSeen(episode: Episode) {
        val episodeSeen = episode.episode_number.toInt()

        val trackManager = Injekt.get<TrackManager>()

        launchIO {
            db.getTracks(anime).executeAsBlocking()
                .mapNotNull { track ->
                    val service = trackManager.getService(track.sync_id)
                    if (service != null && service.isLogged && episodeSeen > track.last_episode_seen) {
                        track.last_episode_seen = episodeSeen

                        // We want these to execute even if the presenter is destroyed and leaks
                        // for a while. The view can still be garbage collected.
                        async {
                            runCatching {
                                service.update(track)
                                db.insertTrack(track).executeAsBlocking()
                            }
                        }
                    } else {
                        null
                    }
                }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { Timber.w(it) }
        }
    }

    companion object {
        fun newIntent(context: Context, anime: Anime, episode: Episode, episodeList: ArrayList<Episode>): Intent {
            return Intent(context, WatcherActivity::class.java).apply {
                putExtra("anime", anime)
                putExtra("episode", episode)
                putExtra("second", episode.last_second_seen)
                putExtra("episodeList", episodeList)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
}
