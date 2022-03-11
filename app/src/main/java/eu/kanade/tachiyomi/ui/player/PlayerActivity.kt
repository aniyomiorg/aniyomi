package eu.kanade.tachiyomi.ui.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.vkay94.dtpv.DoubleTapPlayerView
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource.HttpDataSourceException
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.job.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.job.DelayedTrackingUpdateJob
import eu.kanade.tachiyomi.databinding.WatcherActivityBinding
import eu.kanade.tachiyomi.databinding.WatcherActivityNoPipBinding
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity.Companion.applyAppTheme
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.listener.SimpleSeekBarListener
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.*

class PlayerActivity : AppCompatActivity() {

    private val ACTION_MEDIA_CONTROL = "media_control"
    private val EXTRA_CONTROL_TYPE = "control_type"
    private val REQUEST_PLAY = 1
    private val REQUEST_PAUSE = 2
    private val CONTROL_TYPE_PLAY = 1
    private val CONTROL_TYPE_PAUSE = 2
    private val REQUEST_PREVIOUS = 3
    private val REQUEST_NEXT = 4
    private val CONTROL_TYPE_PREVIOUS = 3
    private val CONTROL_TYPE_NEXT = 4

    /**
     private val REQUEST_INFORMATION = 5
     private val CONTROL_TYPE_INFORMATION = 5 */
    private var mReceiver: BroadcastReceiver? = null
    private val preferences: PreferencesHelper = Injekt.get()
    private lateinit var binding_no_pip: WatcherActivityNoPipBinding
    private lateinit var binding_yes_pip: WatcherActivityBinding
    private val incognitoMode = preferences.incognitoMode().get()
    private val db: AnimeDatabaseHelper = Injekt.get()
    private val downloadManager: AnimeDownloadManager = Injekt.get()
    private val delayedTrackingStore: DelayedTrackingStore = Injekt.get()
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var dbProvider: StandaloneDatabaseProvider
    private val cacheSize = 100L * 1024L * 1024L // 100 MB
    private var simpleCache: SimpleCache? = null
    private lateinit var cacheFactory: CacheDataSource.Factory
    private lateinit var mediaSourceFactory: MediaSourceFactory
    private lateinit var playerView: DoubleTapPlayerView
    private lateinit var youTubeDoubleTap: YouTubeOverlay
    private lateinit var skipBtn: TextView
    private lateinit var nextBtn: ImageButton
    private lateinit var prevBtn: ImageButton
    private lateinit var backBtn: TextView
    private lateinit var settingsBtn: ImageButton
    private lateinit var fitScreenBtn: ImageButton
    private lateinit var pipBtn: ImageButton
    private lateinit var title: TextView
    private lateinit var bufferingView: ProgressBar

    private lateinit var episode: Episode
    private lateinit var anime: Anime
    private lateinit var source: AnimeSource
    private lateinit var uri: String
    private var videos = emptyList<Video>()
    private var isBuffering = true
    private var isLocal = false
    private var currentQuality = 0

    private var duration: Long = 0
    private var playbackPosition = 0L
    private var isFullscreen = false
    private var isPlayerPlaying = true
    private var mediaItem = MediaItem.Builder()
        .setUri("bruh")
        .setMimeType(MimeTypes.VIDEO_MP4)
        .build()

    private var isInPipMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!preferences.pipPlayerPreference()) {
            binding_no_pip = WatcherActivityNoPipBinding.inflate(layoutInflater)
            applyAppTheme(preferences)
            super.onCreate(savedInstanceState)

            setContentView(binding_no_pip.root)

            playerView = binding_no_pip.playerView
            playerView.resizeMode = preferences.getPlayerViewMode()
            youTubeDoubleTap = binding_no_pip.youtubeOverlay
            youTubeDoubleTap.seekSeconds(preferences.skipLengthPreference())
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
            backBtn = findViewById(R.id.exo_overlay_back)
            title = findViewById(R.id.exo_overlay_title)
            skipBtn = findViewById(R.id.watcher_controls_skip_btn)
            nextBtn = findViewById(R.id.watcher_controls_next)
            prevBtn = findViewById(R.id.watcher_controls_prev)
            settingsBtn = findViewById(R.id.watcher_controls_settings)
            fitScreenBtn = findViewById(R.id.watcher_controls_fit_screen)
            bufferingView = findViewById(R.id.exo_buffering)

            anime = intent.getSerializableExtra("anime") as Anime
            episode = intent.getSerializableExtra("episode") as Episode
            title.text = baseContext.getString(R.string.playertitle, anime.title, episode.name)
            source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
            initDummyPlayer()

            if (savedInstanceState != null) {
                playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
                isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
                isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
            }
        } else {
            binding_yes_pip = WatcherActivityBinding.inflate(layoutInflater)
            applyAppTheme(preferences)
            super.onCreate(savedInstanceState)

            setContentView(binding_yes_pip.root)

            playerView = binding_yes_pip.playerView
            playerView.resizeMode = preferences.getPlayerViewMode()
            youTubeDoubleTap = binding_yes_pip.youtubeOverlay
            youTubeDoubleTap.seekSeconds(preferences.skipLengthPreference())
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
            backBtn = findViewById(R.id.exo_overlay_back)
            title = findViewById(R.id.exo_overlay_title)
            skipBtn = findViewById(R.id.watcher_controls_skip_btn)
            nextBtn = findViewById(R.id.watcher_controls_next)
            prevBtn = findViewById(R.id.watcher_controls_prev)
            settingsBtn = findViewById(R.id.watcher_controls_settings)
            fitScreenBtn = findViewById(R.id.watcher_controls_fit_screen)
            pipBtn = findViewById(R.id.watcher_controls_pip)
            bufferingView = findViewById(R.id.exo_buffering)

            anime = intent.getSerializableExtra("anime") as Anime
            episode = intent.getSerializableExtra("episode") as Episode
            title.text = baseContext.getString(R.string.playertitle, anime.title, episode.name)
            source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
            initDummyPlayer()

            if (savedInstanceState != null) {
                playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
                isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
                isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setVisibilities() {
        // TODO: replace this atrocity
        if (!preferences.pipPlayerPreference()) {
            binding_no_pip.root.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        } else {
            binding_yes_pip.root.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun initDummyPlayer() {
        mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this))
        exoPlayer = newPlayer()
        dbProvider = StandaloneDatabaseProvider(baseContext)
        val cacheFolder = File(baseContext.filesDir, "media")
        simpleCache = if (SimpleCache.isCacheFolderLocked(cacheFolder)) {
            null
        } else {
            SimpleCache(
                cacheFolder,
                LeastRecentlyUsedCacheEvictor(cacheSize),
                dbProvider
            )
        }

        initPlayer()
    }

    private fun onGetLinksError(e: Throwable? = null) {
        launchUI {
            baseContext.toast(e?.message ?: "error getting links", Toast.LENGTH_LONG)
            finish()
        }
    }

    private fun onGetLinks() {
        val context = this
        launchUI {
            isBuffering(false)
            if (videos.isEmpty()) {
                onGetLinksError(Exception("Couldn't find any video links."))
                return@launchUI
            }
            dbProvider = StandaloneDatabaseProvider(baseContext)
            isLocal = EpisodeLoader.isDownloaded(episode, anime) || source is LocalAnimeSource
            if (isLocal) {
                uri = videos.firstOrNull()?.uri?.toString() ?: return@launchUI onGetLinksError(Exception("URI is null."))
                dataSourceFactory = DefaultDataSource.Factory(context)
            } else {
                uri = videos.firstOrNull()?.videoUrl ?: return@launchUI onGetLinksError(Exception("video URL is null."))
                dataSourceFactory = newDataSourceFactory()
            }
            logcat(LogPriority.INFO) { "playing $uri" }
            if (simpleCache != null) {
                cacheFactory = CacheDataSource.Factory().apply {
                    setCache(simpleCache!!)
                    setUpstreamDataSourceFactory(dataSourceFactory)
                }
                mediaSourceFactory = DefaultMediaSourceFactory(cacheFactory)
            } else {
                mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            }
            mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(getMime(uri))
                .build()
            playbackPosition = episode.last_second_seen
            changePlayer(playbackPosition, isLocal)
        }
    }

    private fun newDataSourceFactory(): DefaultHttpDataSource.Factory {
        val defaultUserAgentString = WebSettings.getDefaultUserAgent(baseContext)
        return DefaultHttpDataSource.Factory().apply {
            val currentHeaders = videos.getOrNull(currentQuality)?.headers
            val headers = currentHeaders?.toMultimap()
                ?.mapValues { it.value.getOrNull(0) ?: "" }
                ?.toMutableMap()
                ?: (source as AnimeHttpSource).headers.toMultimap()
                    .mapValues { it.value.getOrNull(0) ?: "" }
                    .toMutableMap()
            setDefaultRequestProperties(headers)
            setUserAgent(headers["user-agent"] ?: defaultUserAgentString)
        }
    }

    private fun getMime(uri: String): String {
        return when (Uri.parse(uri).path?.substringAfterLast(".")) {
            "mp4" -> MimeTypes.VIDEO_MP4
            "mkv" -> MimeTypes.VIDEO_MATROSKA
            "m3u8" -> MimeTypes.APPLICATION_M3U8
            else -> MimeTypes.VIDEO_MP4
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    private fun initPlayer() {
        exoPlayer = newPlayer().apply {
            playWhenReady = isPlayerPlaying
            prepare()
        }
        exoPlayer.addListener(PlayerEventListener(playerView, baseContext))

        backBtn.setOnClickListener {
            onBackPressed()
        }
        fitScreenBtn.setOnClickListener {
            onClickFitScreen()
        }
        if (preferences.pipPlayerPreference()) {
            pipBtn.setOnClickListener {
                startPiP()
            }
        }
        settingsBtn.setOnClickListener {
            optionsDialog()
        }
        skipBtn.setOnClickListener {
            exoPlayer.seekTo(exoPlayer.currentPosition + 85000)
        }
        nextBtn.setOnClickListener {
            nextEpisode()
        }
        prevBtn.setOnClickListener {
            previousEpisode()
        }
        youTubeDoubleTap.player(exoPlayer)
        playerView.player = exoPlayer
        duration = exoPlayer.duration
        awaitVideoList()
    }

    private fun isBuffering(param: Boolean) {
        isBuffering = param
        if (param) {
            bufferingView.visibility = View.VISIBLE
        } else {
            bufferingView.visibility = View.GONE
        }
    }

    private fun onClickFitScreen() {
        playerView.resizeMode = when (playerView.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        preferences.setPlayerViewMode(playerView.resizeMode)
    }

    private inner class HideBarsMaterialAlertDialogBuilder(context: Context) : MaterialAlertDialogBuilder(context) {
        override fun create(): AlertDialog {
            return super.create().apply {
                val window = this.window ?: return@apply
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

                val alertWindowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                alertWindowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                alertWindowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun optionsDialog() {
        val alert = HideBarsMaterialAlertDialogBuilder(this)

        alert.setTitle(R.string.playback_options_title)
        alert.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.cancel()
        }
        alert.setItems(R.array.playback_options) { _, which ->
            when (which) {
                0 -> {
                    val speedAlert = HideBarsMaterialAlertDialogBuilder(this)

                    val linear = LinearLayout(this)

                    val items = arrayOf(
                        "10%", "25%", "40%", "50%", "60%", "70%", "75%", "80%",
                        "85%", "90%", "95%", "100% (Normal)", "105%", "110%", "115%", "120%", "125%",
                        "130%", "140%", "150%", "175%", "200%", "250%", "300%"
                    )
                    val floatItems = arrayOf(
                        0.1F, 0.25F, 0.4F, 0.5F, 0.6F, 0.7F, 0.75F, 0.8F, 0.85F, 0.9F,
                        0.95F, 1F, 1.05F, 1.1F, 1.15F, 1.2F, 1.25F, 1.3F, 1.4F, 1.5F, 1.75F, 2F, 2.5F, 3F
                    )
                    var newSpeed = preferences.getPlayerSpeed()

                    speedAlert.setTitle(R.string.playback_speed_dialog_title)

                    linear.orientation = LinearLayout.VERTICAL
                    val text = TextView(this)
                    text.text = items[floatItems.indexOf(newSpeed)]
                    text.setPadding(30, 10, 10, 10)

                    val seek = SeekBar(this).apply {
                        max = items.lastIndex
                        progress = floatItems.indexOf(newSpeed)
                    }
                    seek.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
                        override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                            text.text = items[value]
                            newSpeed = floatItems[value]
                        }
                    })

                    linear.addView(seek)
                    linear.addView(text)

                    speedAlert.setView(linear)

                    speedAlert.setPositiveButton(android.R.string.ok) { speedDialog, _ ->
                        exoPlayer.playbackParameters = PlaybackParameters(newSpeed)
                        preferences.setPlayerSpeed(newSpeed)
                        speedDialog.dismiss()
                    }

                    speedAlert.setNegativeButton(android.R.string.cancel) { speedDialog, _ ->
                        speedDialog.cancel()
                    }

                    speedAlert.setNeutralButton(R.string.playback_speed_dialog_reset) { _, _ ->
                        newSpeed = 1F
                        val newProgress = floatItems.indexOf(newSpeed)
                        text.text = items[newProgress]
                        seek.progress = newProgress
                        exoPlayer.playbackParameters = PlaybackParameters(newSpeed)
                        preferences.setPlayerSpeed(newSpeed)
                    }

                    speedAlert.show()
                }
                1 -> {
                    if (videos.isNotEmpty()) {
                        val qualityAlert = HideBarsMaterialAlertDialogBuilder(this)

                        qualityAlert.setTitle(R.string.playback_quality_dialog_title)

                        var requestedQuality = 0
                        val qualities = videos.map { it.quality }.toTypedArray()
                        qualityAlert.setSingleChoiceItems(qualities, currentQuality) { qualityDialog, selectedQuality ->
                            if (selectedQuality > qualities.lastIndex) {
                                qualityDialog.cancel()
                            } else {
                                requestedQuality = selectedQuality
                            }
                        }

                        qualityAlert.setPositiveButton(android.R.string.ok) { qualityDialog, _ ->
                            if (requestedQuality != currentQuality) changeQuality(requestedQuality)
                            qualityDialog.dismiss()
                        }

                        qualityAlert.setNegativeButton(android.R.string.cancel) { qualityDialog, _ ->
                            qualityDialog.cancel()
                        }

                        qualityAlert.show()
                    }
                }
                else -> {
                }
            }
        }
        alert.show()
    }

    private fun releasePlayer() {
        youTubeDoubleTap.player(exoPlayer)
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        exoPlayer.release()
    }

    private fun awaitVideoList() {
        isBuffering(true)
        launchIO {
            try {
                EpisodeLoader.getLinks(episode, anime, source)
                    .doOnNext {
                        videos = it
                        onGetLinks()
                    }.awaitSingle()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { e.message ?: "error getting links" }
                onGetLinksError(e)
            }
        }
    }

    private fun nextEpisode() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
        saveEpisodeHistory(EpisodeItem(episode, anime))
        setEpisodeProgress(episode, anime, exoPlayer.currentPosition, exoPlayer.duration)
        val oldEpisode = episode
        episode = getNextEpisode(episode, anime)
        if (oldEpisode == episode) {
            launchUI {
                baseContext.toast(R.string.no_next_episode, Toast.LENGTH_SHORT)
            }
            return
        }

        title.text = baseContext.getString(R.string.playertitle, anime.title, episode.name)
        currentQuality = 0
        awaitVideoList()
    }

    private fun previousEpisode() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
        saveEpisodeHistory(EpisodeItem(episode, anime))
        setEpisodeProgress(episode, anime, exoPlayer.currentPosition, exoPlayer.duration)
        val oldEpisode = episode
        episode = getPreviousEpisode(episode, anime)
        if (oldEpisode == episode) {
            launchUI {
                baseContext.toast(R.string.no_previous_episode, Toast.LENGTH_SHORT)
            }
            return
        }
        title.text = baseContext.getString(R.string.playertitle, anime.title, episode.name)
        currentQuality = 0
        awaitVideoList()
    }

    private fun changeQuality(quality: Int) {
        baseContext.toast(videos.getOrNull(quality)?.quality, Toast.LENGTH_SHORT)
        uri = if (isLocal) {
            videos.getOrNull(quality)?.uri?.toString() ?: return
        } else {
            videos.getOrNull(quality)?.videoUrl ?: return
        }
        currentQuality = quality
        mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(getMime(uri))
            .build()
        changePlayer(exoPlayer.currentPosition, isLocal)
    }

    private fun changePlayer(resumeAt: Long, isLocal: Boolean) {
        dataSourceFactory = if (isLocal) {
            DefaultDataSource.Factory(this)
        } else {
            newDataSourceFactory()
        }
        if (simpleCache != null) {
            cacheFactory = CacheDataSource.Factory().apply {
                setCache(simpleCache!!)
                setUpstreamDataSourceFactory(dataSourceFactory)
            }
            mediaSourceFactory = DefaultMediaSourceFactory(cacheFactory)
        } else {
            mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        }
        exoPlayer.release()
        exoPlayer = newPlayer()
        exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem), resumeAt)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.addListener(PlayerEventListener(playerView, baseContext))
        youTubeDoubleTap.player(exoPlayer)
        playerView.player = exoPlayer
    }

    private fun newPlayer(): ExoPlayer {
        return ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this),
            mediaSourceFactory,
            DefaultTrackSelector(this),
            DefaultLoadControl(),
            DefaultBandwidthMeter.Builder(this).build(),
            AnalyticsCollector(Clock.DEFAULT)
        ).setSeekForwardIncrementMs(85000L)
            .build().apply {
                playbackParameters = PlaybackParameters(preferences.getPlayerSpeed())
            }
    }

    class PlayerEventListener(private val playerView: DoubleTapPlayerView, val baseContext: Context) : Player.Listener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            playerView.keepScreenOn = !(
                playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED ||
                    !playWhenReady
                )
        }
        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}
        override fun onLoadingChanged(isLoading: Boolean) {}
        override fun onRepeatModeChanged(repeatMode: Int) {}
        override fun onPlayerError(error: PlaybackException) {
            val cause: Throwable? = error.cause
            if (cause is HttpDataSourceException) {
                // An HTTP error occurred.
                // This is the request for which the error occurred.
                val requestDataSpec = cause.dataSpec
                for (header in requestDataSpec.httpRequestHeaders) {
                    var message = ""
                    message += header.key + " - " + header.value
                }
                // It's possible to find out more about the error both by casting and by
                // querying the cause.
                if (cause is InvalidResponseCodeException) {
                    // Cast to InvalidResponseCodeException and retrieve the response code,
                    // message and headers.
                    val errorMessage =
                        "Error " + cause.responseCode.toString() + ": " + cause.message
                    baseContext.toast(errorMessage, Toast.LENGTH_SHORT)
                } else {
                    cause.cause
                    // Try calling httpError.getCause() to retrieve the underlying cause,
                    // although note that it may be null.
                }
            }
        }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        playerView.onResume()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        setVisibilities()
        updatePictureInPictureActions(exoPlayer.isPlaying)
    }

    override fun onStop() {
        saveEpisodeHistory(EpisodeItem(episode, anime))
        setEpisodeProgress(episode, anime, exoPlayer.currentPosition, exoPlayer.duration)
        if (exoPlayer.isPlaying) exoPlayer.pause()
        // if (isInPipMode) finish()
        playerView.onPause()
        updatePictureInPictureActions(exoPlayer.isPlaying)
        super.onStop()
    }

    override fun onDestroy() {
        deletePendingEpisodes()
        releasePlayer()
        simpleCache?.release()
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            finishAndRemoveTask()
        }
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        if (exoPlayer.isPlaying) {
            startPiP()
        }

        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val oldAnime = anime
        anime = intent?.getSerializableExtra("anime") as? Anime ?: return
        val oldEpisode = episode
        episode = intent.getSerializableExtra("episode") as Episode
        if (oldEpisode == episode) return
        saveEpisodeHistory(EpisodeItem(oldEpisode, oldAnime))
        setEpisodeProgress(oldEpisode, oldAnime, exoPlayer.currentPosition, exoPlayer.duration)
        title.text = baseContext.getString(R.string.playertitle, anime.title, episode.name)
        source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
        awaitVideoList()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        isInPipMode = isInPictureInPictureMode
        playerView.useController = !isInPictureInPictureMode
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // On Android TV it is required to hide controller in this PIP change callback
            playerView.hideController()
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null || ACTION_MEDIA_CONTROL != intent.action) {
                        return
                    }
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        CONTROL_TYPE_PLAY -> {
                            exoPlayer.play()
                            updatePictureInPictureActions(exoPlayer.isPlaying)
                        }
                        CONTROL_TYPE_PAUSE -> {
                            exoPlayer.pause()
                            updatePictureInPictureActions(exoPlayer.isPlaying)
                        }
                        CONTROL_TYPE_PREVIOUS -> {
                            previousEpisode()
                            updatePictureInPictureActions(exoPlayer.isPlaying)
                        }
                        CONTROL_TYPE_NEXT -> {
                            nextEpisode()
                            updatePictureInPictureActions(exoPlayer.isPlaying)
                        }
                        /**CONTROL_TYPE_INFORMATION -> {
                         baseContext.toast(baseContext.getString(R.string.playertitle, anime.title, episode.name), Toast.LENGTH_SHORT)
                         updatePictureInPictureActions(exoPlayer.isPlaying)
                         }         */
                    }
                }
            }
            registerReceiver(mReceiver, IntentFilter(ACTION_MEDIA_CONTROL))
        } else {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver)
                mReceiver = null
            }
            playerView.controllerAutoShow = true

            /**if (exoPlayer != null) {
             if (exoPlayer.isPlaying()) Utils.toggleSystemUi(
             this,
             playerView,
             false
             ) else playerView.showController()
             }*/
        }
    }

    @Suppress("DEPRECATION")
    private fun startPiP() {
        if (!preferences.pipPlayerPreference()) return

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            playerView.useController = false
            this.enterPictureInPictureMode(updatePictureInPictureActions(exoPlayer.isPlaying))
        }
    }

    private fun createRemoteAction(
        iconResId: Int,
        titleResId: Int,
        requestCode: Int,
        controlType: Int
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(this, iconResId),
            getString(titleResId),
            getString(titleResId),
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(ACTION_MEDIA_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    fun updatePictureInPictureActions(
        playing: Boolean
    ): PictureInPictureParams {
        val mPictureInPictureParams = PictureInPictureParams.Builder()
            // Set action items for the picture-in-picture mode. These are the only custom controls
            // available during the picture-in-picture mode.
            .setActions(
                arrayListOf(
                    /**
                     createRemoteAction(
                     R.drawable.ic_watcher_info_32dp,
                     R.string.action_previous_episode,
                     CONTROL_TYPE_INFORMATION,
                     REQUEST_INFORMATION
                     ),    */

                    createRemoteAction(
                        R.drawable.exo_controls_previous,
                        R.string.action_next_episode,
                        CONTROL_TYPE_PREVIOUS,
                        REQUEST_PREVIOUS
                    ),

                    if (playing) {
                        // "Pause" action when the stopwatch is already started.
                        createRemoteAction(
                            R.drawable.exo_controls_pause,
                            R.string.exo_controls_pause_description,
                            CONTROL_TYPE_PAUSE,
                            REQUEST_PAUSE
                        )
                    } else {
                        // "Start" action when the stopwatch is not started.
                        createRemoteAction(
                            R.drawable.exo_controls_play,
                            R.string.exo_controls_play_description,
                            CONTROL_TYPE_PLAY,
                            REQUEST_PLAY
                        )
                    },
                    createRemoteAction(
                        R.drawable.exo_controls_next,
                        R.string.action_player_information,
                        CONTROL_TYPE_NEXT,
                        REQUEST_NEXT
                    )

                )
            )
            .build()
        setPictureInPictureParams(mPictureInPictureParams)
        return mPictureInPictureParams
    }
    private fun saveEpisodeHistory(episode: EpisodeItem) {
        if (!incognitoMode && !isBuffering) {
            val history = AnimeHistory.create(episode.episode).apply { last_seen = Date().time }
            db.updateAnimeHistoryLastSeen(history).asRxCompletable()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    private fun setEpisodeProgress(episode: Episode, anime: Anime, seconds: Long, totalSeconds: Long) {
        if (!incognitoMode && !isBuffering) {
            if (totalSeconds > 0L) {
                episode.last_second_seen = seconds
                episode.total_seconds = totalSeconds
                val progress = preferences.progressPreference()
                if (!episode.seen) episode.seen = episode.last_second_seen >= episode.total_seconds * progress
                val episodes = listOf(EpisodeItem(episode, anime))
                launchIO {
                    db.updateEpisodesProgress(episodes).executeAsBlocking()
                    if (preferences.autoUpdateTrack() && episode.seen) {
                        updateTrackEpisodeSeen(episode)
                    }
                    if (episode.seen) {
                        deleteEpisodeIfNeeded(episode)
                        deleteEpisodeFromDownloadQueue(episode)
                    }
                }
            }
        }
    }

    private fun deleteEpisodeFromDownloadQueue(episode: Episode) {
        downloadManager.getEpisodeDownloadOrNull(episode)?.let { download ->
            downloadManager.deletePendingDownload(download)
        }
    }

    private fun enqueueDeleteSeenEpisodes(episode: Episode) {
        if (!episode.seen) return
        val anime = anime

        launchIO {
            downloadManager.enqueueDeleteEpisodes(listOf(episode), anime)
        }
    }

    private fun deleteEpisodeIfNeeded(episode: Episode) {
        // Determine which chapter should be deleted and enqueue
        val sortFunction: (Episode, Episode) -> Int = when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Anime.EPISODE_SORTING_NUMBER -> { c1, c2 -> c1.episode_number.compareTo(c2.episode_number) }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val episodes = db.getEpisodes(anime).executeAsBlocking()
            .sortedWith { e1, e2 -> sortFunction(e1, e2) }

        val currentEpisodePosition = episodes.indexOf(episode)
        val removeAfterReadSlots = preferences.removeAfterReadSlots()
        val episodeToDelete = episodes.getOrNull(currentEpisodePosition - removeAfterReadSlots)

        // Check if deleting option is enabled and chapter exists
        if (removeAfterReadSlots != -1 && episodeToDelete != null) {
            enqueueDeleteSeenEpisodes(episodeToDelete)
        }
    }

    /**
     * Deletes all the pending episodes. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingEpisodes() {
        launchIO {
            downloadManager.deletePendingEpisodes()
        }
    }

    private fun updateTrackEpisodeSeen(episode: Episode) {
        val episodeSeen = episode.episode_number

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
                                if (baseContext.isOnline()) {
                                    service.update(track, true)
                                    db.insertTrack(track).executeAsBlocking()
                                } else {
                                    delayedTrackingStore.addItem(track)
                                    DelayedTrackingUpdateJob.setupTask(baseContext)
                                }
                            }
                        }
                    } else {
                        null
                    }
                }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { logcat(LogPriority.WARN, it) }
        }
    }

    private fun getNextEpisode(episode: Episode, anime: Anime): Episode {
        val sortFunction: (Episode, Episode) -> Int = when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Anime.EPISODE_SORTING_NUMBER -> { c1, c2 -> c1.episode_number.compareTo(c2.episode_number) }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val episodes = db.getEpisodes(anime).executeAsBlocking()
            .sortedWith { e1, e2 -> sortFunction(e1, e2) }

        val currEpisodeIndex = episodes.indexOfFirst { episode.id == it.id }
        val episodeNumber = episode.episode_number
        return (currEpisodeIndex + 1 until episodes.size)
            .map { episodes[it] }
            .firstOrNull {
                it.episode_number > episodeNumber &&
                    it.episode_number <= episodeNumber + 1
            } ?: episode
    }

    private fun getPreviousEpisode(episode: Episode, anime: Anime): Episode {
        val sortFunction: (Episode, Episode) -> Int = when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Anime.EPISODE_SORTING_NUMBER -> { c1, c2 -> c1.episode_number.compareTo(c2.episode_number) }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val episodes = db.getEpisodes(anime).executeAsBlocking()
            .sortedWith { e1, e2 -> sortFunction(e2, e1) }

        val currEpisodeIndex = episodes.indexOfFirst { episode.id == it.id }
        val episodeNumber = episode.episode_number
        return (currEpisodeIndex + 1 until episodes.size)
            .map { episodes[it] }
            .firstOrNull {
                it.episode_number < episodeNumber &&
                    it.episode_number >= episodeNumber - 1
            } ?: episode
    }

    companion object {
        fun newIntent(context: Context, anime: Anime, episode: Episode): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra("anime", anime)
                putExtra("episode", episode)
                putExtra("second", episode.last_second_seen)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }
}

private const val STATE_RESUME_POSITION = "resumePosition"
private const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
private const val STATE_PLAYER_PLAYING = "playerOnPlay"
