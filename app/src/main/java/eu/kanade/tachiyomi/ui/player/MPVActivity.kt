package eu.kanade.tachiyomi.ui.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import `is`.xyz.mpv.BackgroundPlaybackService
import `is`.xyz.mpv.DecimalPickerDialog
import `is`.xyz.mpv.FilePickerActivity
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVView
import `is`.xyz.mpv.NotificationButtonReceiver
import `is`.xyz.mpv.PickerDialog
import `is`.xyz.mpv.PlaylistDialog
import `is`.xyz.mpv.PropertyChange
import `is`.xyz.mpv.SliderPickerDialog
import `is`.xyz.mpv.SpeedPickerDialog
import `is`.xyz.mpv.TouchGestures
import `is`.xyz.mpv.TouchGesturesObserver
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.databinding.PlayerBinding
import java.io.File
import java.lang.IllegalArgumentException
import kotlin.math.roundToInt

typealias ActivityResultCallback = (Int, Intent?) -> Unit
typealias StateRestoreCallback = () -> Unit

class MPVActivity : AppCompatActivity(), MPVLib.EventObserver, TouchGesturesObserver {
    private val fadeHandler = Handler()
    private val stopServiceHandler = Handler()

    /**
     * DO NOT USE THIS
     */
    private var activityIsStopped = false

    private var activityIsForeground = true
    private var didResumeBackgroundPlayback = false

    private var userIsOperatingSeekbar = false

    private var audioManager: AudioManager? = null
    private var audioFocusRestore: () -> Unit = {}

    private lateinit var binding: PlayerBinding
    private lateinit var toast: Toast
    private lateinit var gestures: TouchGestures

    // convenience alias
    private val player get() = binding.player

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

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { type ->
        Log.v(TAG, "Audio focus changed: $type")
        if (ignoreAudioFocus) {
            return@OnAudioFocusChangeListener
        }
        when (type) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // loss can occur in addition to ducking, so remember the old callback
                val oldRestore = audioFocusRestore
                val wasPlayerPaused = player.paused ?: false
                player.paused = true
                audioFocusRestore = {
                    oldRestore()
                    if (!wasPlayerPaused) player.paused = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                MPVLib.command(arrayOf("multiply", "volume", AUDIO_FOCUS_DUCKING.toString()))
                audioFocusRestore = {
                    val inv = 1f / AUDIO_FOCUS_DUCKING
                    MPVLib.command(arrayOf("multiply", "volume", inv.toString()))
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusRestore()
                audioFocusRestore = {}
            }
        }
    }

    // Fade out controls
    private val fadeRunnable = object : Runnable {
        var hasStarted = false
        private val listener = object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) { hasStarted = true }

            override fun onAnimationCancel(animation: Animator?) { hasStarted = false }

            override fun onAnimationEnd(animation: Animator) {
                if (hasStarted) {
                    hideControls()
                }
                hasStarted = false
            }
        }

        override fun run() {
            binding.topControls.animate().alpha(0f).setDuration(CONTROLS_FADE_DURATION)
            binding.controls.animate().alpha(0f).setDuration(CONTROLS_FADE_DURATION).setListener(listener)
        }
    }

    // Fade out unlock button
    private val fadeRunnable2 = object : Runnable {
        private val listener = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.unlockBtn.visibility = View.GONE
            }
        }

        override fun run() {
            binding.unlockBtn.animate().alpha(0f).setDuration(CONTROLS_FADE_DURATION).setListener(listener)
        }
    }

    // Fade out gesture text
    private val fadeRunnable3 = object : Runnable {
        // okay this doesn't actually fade...
        override fun run() {
            binding.gestureTextView.visibility = View.GONE
        }
    }

    private val stopServiceRunnable = Runnable {
        val intent = Intent(this, BackgroundPlaybackService::class.java)
        applicationContext.stopService(intent)
    }

    /* Settings */
    private var statsEnabled = false
    private var statsOnlyFPS = false
    private var statsLuaMode = 0 // ==0 disabled, >0 page number

    private var backgroundPlayMode = ""

    private var shouldSavePosition = false

    private var autoRotationMode = ""

    private var controlsAtBottom = false
    private var showMediaTitle = false

    private var ignoreAudioFocus = false
    /* * */

    private fun initListeners() {
        with(binding) {
            cycleAudioBtn.setOnLongClickListener { pickAudio(); true }
            cycleSpeedBtn.setOnLongClickListener { pickSpeed(); true }
            cycleSubsBtn.setOnLongClickListener { pickSub(); true }

            prevBtn.setOnLongClickListener { openPlaylistMenu(pauseForDialog()); true }
            nextBtn.setOnLongClickListener { openPlaylistMenu(pauseForDialog()); true }
        }
    }

    @SuppressLint("ShowToast")
    private fun initMessageToast() {
        toast = Toast.makeText(this, "This totally shouldn't be seen", Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 0)
    }

    private var playbackHasStarted = false
    private var onloadCommands = mutableListOf<Array<String>>()

    // Activity lifetime

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Do these here and not in MainActivity because mpv can be launched from a file browser
        Utils.copyAssets(this)
        BackgroundPlaybackService.createNotificationChannel(this)

        binding = PlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init controls to be hidden and view fullscreen
        hideControls()

        // Initialize listeners for the player view
        initListeners()

        // Initialize toast used for short messages
        initMessageToast()

        gestures = TouchGestures(this)

        // set up initial UI state
        syncSettings()
        onConfigurationChanged(resources.configuration)
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            binding.topPiPBtn.visibility = View.GONE
        }

        if (showMediaTitle) {
            binding.controlsTitleGroup.visibility = View.VISIBLE
        }

        updateOrientation(true)

        // Parse the intent
        val filepath = parsePathFromIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            parseIntentExtras(intent.extras)
        }

        if (filepath == null) {
            Log.e(TAG, "No file given, exiting")
            showToast(getString(R.string.error_no_file))
            finishWithResult(RESULT_CANCELED)
            return
        }

        player.initialize(applicationContext.filesDir.path)
        player.addObserver(this)
        player.playFile(filepath)

        binding.playbackSeekbar.setOnSeekBarChangeListener(seekBarChangeListener)

        player.setOnTouchListener { _, e ->
            if (lockedUI) false else gestures.onTouchEvent(e)
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        volumeControlStream = AudioManager.STREAM_MUSIC

        @Suppress("DEPRECATION")
        val res = audioManager!!.requestAudioFocus(
            audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
        if (res != AudioManager.AUDIOFOCUS_REQUEST_GRANTED && !ignoreAudioFocus) {
            Log.w(TAG, "Audio focus not granted")
            onloadCommands.add(arrayOf("set", "pause", "yes"))
        }
    }

    private fun finishWithResult(code: Int, includeTimePos: Boolean = false) {
        /*
         * Description of result intent (inspired by https://mx.j2inter.com/api)
         * ============================
         * action: constant "is.xyz.mpv.MPVActivity.result"
         * code:
         *   RESULT_CANCELED: playback did not start due to an error
         *   RESULT_OK: playback ended normally or user exited
         * data: same URI mpv was started with
         * extras:
         *   "position" (int): last playback pos in milliseconds, missing if playback finished normally
         *   "duration" (int): total playback length in milliseconds, missing if playback finished normally
         */
        // FIXME: should track end-file events to accurately report OK vs CANCELED
        val result = Intent(RESULT_INTENT)
        result.data = if (intent.data?.scheme == "file") null else intent.data
        if (includeTimePos) {
            MPVLib.getPropertyDouble("time-pos")?.let {
                result.putExtra("position", (it * 1000f).toInt())
            }
            MPVLib.getPropertyDouble("duration")?.let {
                result.putExtra("duration", (it * 1000f).toInt())
            }
        }
        setResult(code, result)
        finish()
    }

    override fun onDestroy() {
        Log.v(TAG, "Exiting.")

        @Suppress("DEPRECATION")
        audioManager?.abandonAudioFocus(audioFocusChangeListener)

        // take the background service with us
        stopServiceRunnable.run()

        player.removeObserver(this)
        player.destroy()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        Log.v(TAG, "onNewIntent($intent)")
        super.onNewIntent(intent)

        // Happens when mpv is still running (not necessarily playing) and the user selects a new
        // file to be played from another app
        val filepath = intent?.let { parsePathFromIntent(it) }
        if (filepath == null) {
            return
        }

        if (!activityIsForeground && didResumeBackgroundPlayback) {
            MPVLib.command(arrayOf("loadfile", filepath, "append"))
            showToast(getString(R.string.notice_file_appended))
            moveTaskToBack(true)
        } else {
            MPVLib.command(arrayOf("loadfile", filepath))
        }
    }

    private fun isPlayingAudioOnly(): Boolean {
        if (player.aid == -1) {
            return false
        }
        val fmt = MPVLib.getPropertyString("video-format")
        return fmt.isNullOrEmpty() || arrayOf("mjpeg", "png", "bmp").indexOf(fmt) != -1
    }

    private fun shouldBackground(): Boolean {
        if (isFinishing) { // about to exit?
            return false
        }
        if (player.paused ?: true) {
            return false
        }
        return when (backgroundPlayMode) {
            "always" -> true
            "audio-only" -> isPlayingAudioOnly()
            else -> false // "never"
        }
    }

    override fun onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInMultiWindowMode || isInPictureInPictureMode) {
                Log.v(TAG, "Going into multi-window mode (PiP=$isInPictureInPictureMode)")
                super.onPause()
                return
            }
        }

        onPauseImpl()
    }

    private fun onPauseImpl() {
        val fmt = MPVLib.getPropertyString("video-format")
        val shouldBackground = shouldBackground()
        if (shouldBackground && !fmt.isNullOrEmpty()) {
            BackgroundPlaybackService.thumbnail = MPVLib.grabThumbnail(THUMB_SIZE)
        } else {
            BackgroundPlaybackService.thumbnail = null
        }

        activityIsForeground = false
        if (isFinishing) {
            savePosition()
            MPVLib.command(arrayOf("stop"))
        } else if (!shouldBackground) {
            player.paused = true
        }
        super.onPause()

        didResumeBackgroundPlayback = shouldBackground
        if (shouldBackground) {
            Log.v(TAG, "Resuming playback in background")
            stopServiceHandler.removeCallbacks(stopServiceRunnable)
            val serviceIntent = Intent(this, BackgroundPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun syncSettings() {
        // FIXME: settings should be in their own class completely
        val prefs = getDefaultSharedPreferences(applicationContext)
        val getString: (String, Int) -> String = { key, defaultRes ->
            prefs.getString(key, resources.getString(defaultRes))!!
        }

        gestures.syncSettings(prefs, resources)

        val statsMode = prefs.getString("stats_mode", "")
        if (statsMode.isNullOrBlank()) {
            this.statsEnabled = false
        } else if (statsMode == "native" || statsMode == "native_fps") {
            this.statsEnabled = true
            this.statsOnlyFPS = statsMode == "native_fps"
        } else if (statsMode.startsWith("lua")) {
            this.statsEnabled = false
            this.statsLuaMode = statsMode.removePrefix("lua").toInt()
        }
        this.backgroundPlayMode = getString("background_play", R.string.pref_background_play_default)
        this.shouldSavePosition = prefs.getBoolean("save_position", false)
        this.autoRotationMode = getString("auto_rotation", R.string.pref_auto_rotation_default)
        this.controlsAtBottom = prefs.getBoolean("bottom_controls", false)
        this.showMediaTitle = prefs.getBoolean("display_media_title", false)
        this.ignoreAudioFocus = prefs.getBoolean("ignore_audio_focus", false)

        // Apply some changes depending on preferences

        if (this.statsOnlyFPS) {
            binding.statsTextView.setTextColor((0xFF00FF00).toInt()) // green
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayInCutout = prefs.getBoolean("display_in_cutout", true)
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = if (displayInCutout) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            window.attributes = lp
        }
    }

    override fun onStart() {
        super.onStart()
        activityIsStopped = false
    }

    override fun onStop() {
        super.onStop()
        activityIsStopped = true
    }

    override fun onResume() {
        // If we weren't actually in the background (e.g. multi window mode), don't reinitialize stuff
        if (activityIsForeground) {
            super.onResume()
            return
        }

        if (lockedUI) { // precaution
            Log.w(TAG, "resumed with locked UI, unlocking")
            unlockUI(null)
        }

        // Init controls to be hidden and view fullscreen
        hideControls()
        syncSettings()

        activityIsForeground = true
        // stop background service with a delay
        stopServiceHandler.removeCallbacks(stopServiceRunnable)
        stopServiceHandler.postDelayed(stopServiceRunnable, 1000L)

        refreshUi()

        super.onResume()
    }

    private fun savePosition() {
        if (!shouldSavePosition) {
            return
        }
        if (MPVLib.getPropertyBoolean("eof-reached") ?: true) {
            Log.d(TAG, "player indicates EOF, not saving watch-later config")
            return
        }
        MPVLib.command(arrayOf("write-watch-later-config"))
    }

    // UI

    private var btnSelected = -1 // dpad navigation

    private var mightWantToToggleControls = false

    private var useAudioUI = false
    private var lockedUI = false

    private fun pauseForDialog(): StateRestoreCallback {
        val wasPlayerPaused = player.paused ?: true // default to not changing state
        player.paused = true
        return {
            if (!wasPlayerPaused) {
                player.paused = false
            }
        }
    }

    private fun updateStats() {
        if (this.statsOnlyFPS) {
            binding.statsTextView.text = getString(R.string.ui_fps, player.estimatedVfFps)
            return
        }

        val text = "File: ${player.filename}\n\n" +
            "Video: ${player.videoCodec} hwdec: ${player.hwdecActive}\n" +
            "\tA-V: ${player.avsync}\n" +
            "\tDropped: decoder: ${player.decoderFrameDropCount}, VO: ${player.frameDropCount}\n" +
            "\tFPS: ${player.containerFps} (specified) ${player.estimatedVfFps} (estimated)\n" +
            "\tResolution: ${player.videoW}x${player.videoH}\n\n" +
            "Audio: ${player.audioCodec}\n" +
            "\tSample rate: ${player.audioSampleRate} Hz\n" +
            "\tChannels: ${player.audioChannels}"
        binding.statsTextView.text = text
    }

    private fun controlsShouldBeVisible(): Boolean {
        if (lockedUI) {
            return false
        }
        // If either the audio UI is active or a button is selected for dpad navigation
        // the controls should never hide
        return useAudioUI || btnSelected != -1
    }

    private fun showControls() {
        if (lockedUI) {
            Log.w(TAG, "cannot show UI in locked mode")
            return
        }

        // remove all callbacks that were to be run for fading
        fadeHandler.removeCallbacks(fadeRunnable)
        binding.controls.animate().cancel()
        binding.topControls.animate().cancel()

        // reset controls alpha to be visible
        binding.controls.alpha = 1f
        binding.topControls.alpha = 1f

        if (binding.controls.visibility != View.VISIBLE) {
            binding.controls.visibility = View.VISIBLE
            binding.topControls.visibility = View.VISIBLE

            if (this.statsEnabled) {
                updateStats()
                binding.statsTextView.visibility = View.VISIBLE
            }

            // TODO: what does this do?
            window.decorView.systemUiVisibility = if (useAudioUI) View.SYSTEM_UI_FLAG_LAYOUT_STABLE else 0
        }

        // add a new callback to hide the controls once again
        if (!controlsShouldBeVisible()) {
            fadeHandler.postDelayed(fadeRunnable, CONTROLS_DISPLAY_TIMEOUT)
        }
    }

    fun hideControls() {
        if (controlsShouldBeVisible()) {
            return
        }
        // use GONE here instead of INVISIBLE (which makes more sense) because of Android bug with surface views
        // see http://stackoverflow.com/a/12655713/2606891
        binding.controls.visibility = View.GONE
        binding.topControls.visibility = View.GONE
        binding.statsTextView.visibility = View.GONE

        val flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.decorView.systemUiVisibility = flags
    }

    private fun hideControlsDelayed() {
        fadeHandler.removeCallbacks(fadeRunnable)
        fadeHandler.post(fadeRunnable)
    }

    private fun toggleControls(): Boolean {
        if (lockedUI) {
            return false
        }
        if (controlsShouldBeVisible()) {
            return true
        }
        return if (binding.controls.visibility == View.VISIBLE && !fadeRunnable.hasStarted) {
            hideControlsDelayed()
            false
        } else {
            showControls()
            true
        }
    }

    private fun showUnlockControls() {
        fadeHandler.removeCallbacks(fadeRunnable2)
        binding.unlockBtn.animate().setListener(null).cancel()

        binding.unlockBtn.alpha = 1f
        binding.unlockBtn.visibility = View.VISIBLE

        fadeHandler.postDelayed(fadeRunnable2, CONTROLS_DISPLAY_TIMEOUT)
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        if (lockedUI) {
            showUnlockControls()
            return super.dispatchKeyEvent(ev)
        }

        // try built-in event handler first, forward all other events to libmpv
        val handled = interceptDpad(ev) ||
            (ev.action == KeyEvent.ACTION_DOWN && interceptKeyDown(ev)) ||
            player.onKey(ev)
        if (handled) {
            showControls()
            return true
        }
        return super.dispatchKeyEvent(ev)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        if (lockedUI) {
            return super.dispatchGenericMotionEvent(ev)
        }

        if (ev != null && ev.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
            if (player.onPointerEvent(ev)) {
                return true
            }
            // keep controls visible when mouse moves
            if (ev.actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                showControls()
            }
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (lockedUI) {
            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_DOWN) {
                showUnlockControls()
            }
            return super.dispatchTouchEvent(ev)
        }

        if (super.dispatchTouchEvent(ev)) {
            // reset delay if the event has been handled
            // ideally we'd want to know if the event was delivered to controls, but we can't
            if (binding.controls.visibility == View.VISIBLE && !fadeRunnable.hasStarted) {
                showControls()
            }
            if (ev.action == MotionEvent.ACTION_UP) {
                return true
            }
        }
        if (ev.action == MotionEvent.ACTION_DOWN) {
            mightWantToToggleControls = true
        }
        if (ev.action == MotionEvent.ACTION_UP && mightWantToToggleControls) {
            // on double taps the controls would dis-/reappear too wildly, so don't do that if those are enabled
            if (gestures.usesTapGestures()) {
                showControls()
            } else {
                toggleControls()
            }
        }
        return true
    }

    private fun interceptDpad(ev: KeyEvent): Boolean {
        if (btnSelected == -1) { // UP and DOWN are always grabbed and overriden
            when (ev.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (ev.action == KeyEvent.ACTION_DOWN) { // activate dpad navigation
                        btnSelected = 0
                        updateShowBtnSelected()
                    }
                    return true
                }
            }
            return false
        }
        // only used when dpad navigation is active
        val group1 = binding.controlsButtonGroup
        val group2 = binding.topControls
        val childCountTotal = group1.childCount + group2.childCount
        when (ev.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (ev.action == KeyEvent.ACTION_DOWN) { // deactivate dpad navigation
                    btnSelected = -1
                    updateShowBtnSelected()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (ev.action == KeyEvent.ACTION_DOWN) {
                    btnSelected = (btnSelected + 1) % childCountTotal
                    updateShowBtnSelected()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (ev.action == KeyEvent.ACTION_DOWN) {
                    btnSelected = (childCountTotal + btnSelected - 1) % childCountTotal
                    updateShowBtnSelected()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (ev.action == KeyEvent.ACTION_DOWN) {
                    val childCount = group1.childCount
                    if (btnSelected < childCount) {
                        group1.getChildAt(btnSelected)?.performClick()
                    } else {
                        group2.getChildAt(btnSelected - childCount)?.performClick()
                    }
                }
                return true
            }
        }
        return false
    }

    private fun updateShowBtnSelected() {
        val colorFocused = ContextCompat.getColor(this, R.color.tint_btn_bg_focused)
        val colorNoFocus = ContextCompat.getColor(this, R.color.tint_btn_bg_nofocus)

        val group1 = binding.controlsButtonGroup
        val group2 = binding.topControls
        val childCount = group1.childCount
        for (i in 0 until childCount) {
            val child = group1.getChildAt(i)
            if (i == btnSelected) {
                child.setBackgroundColor(colorFocused)
            } else {
                child.setBackgroundColor(colorNoFocus)
            }
        }
        for (i in 0 until group2.childCount) {
            val child = group2.getChildAt(i)
            if (i == btnSelected - childCount) {
                child.setBackgroundColor(colorFocused)
            } else {
                child.setBackgroundColor(colorNoFocus)
            }
        }
    }

    private fun interceptKeyDown(event: KeyEvent): Boolean {
        // intercept some keys to provide functionality "native" to
        // mpv-android even if libmpv already implements these
        var unhandeled = 0

        when (event.unicodeChar.toChar()) {
            // overrides a default binding:
            'j' -> cycleSub(binding.cycleSubsBtn)
            '#' -> cycleAudio(binding.cycleAudioBtn)

            else -> unhandeled++
        }
        when (event.keyCode) {
            // no default binding:
            KeyEvent.KEYCODE_CAPTIONS -> cycleSub(binding.cycleSubsBtn)
            KeyEvent.KEYCODE_HEADSETHOOK -> player.cyclePause()
            KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK -> cycleAudio(binding.cycleAudioBtn)
            KeyEvent.KEYCODE_INFO -> toggleControls()
            KeyEvent.KEYCODE_MENU -> openTopMenu(binding.controls)
            KeyEvent.KEYCODE_GUIDE -> openTopMenu(binding.controls)
            KeyEvent.KEYCODE_DPAD_CENTER -> player.cyclePause()

            // overrides a default binding:
            KeyEvent.KEYCODE_ENTER -> player.cyclePause()

            else -> unhandeled++
        }

        return unhandeled < 2
    }

    override fun onBackPressed() {
        if (lockedUI) {
            return showUnlockControls()
        }

        val pos = MPVLib.getPropertyInt("playlist-pos") ?: 0
        val count = MPVLib.getPropertyInt("playlist-count") ?: 1
        val notYetPlayed = count - pos - 1
        if (notYetPlayed <= 0) {
            finishWithResult(RESULT_OK, true)
            return
        }

        val restore = pauseForDialog()
        with(AlertDialog.Builder(this)) {
            setMessage(getString(R.string.exit_warning_playlist, notYetPlayed))
            setPositiveButton(R.string.dialog_yes) { dialog, _ ->
                dialog.dismiss()
                finishWithResult(RESULT_OK, true)
            }
            setNegativeButton(R.string.dialog_no) { dialog, _ ->
                dialog.dismiss()
                restore()
            }
            create().show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        val hasSoftwareKeys = Utils.hasSoftwareKeys(this)

        // TODO: figure out if this should be replaced by WindowManager.getCurrentWindowMetrics()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        gestures.setMetrics(dm.widthPixels.toFloat(), dm.heightPixels.toFloat())

        // Move top controls so they don't overlap with System UI
        if (hasSoftwareKeys) {
            val lp = binding.topControls.layoutParams as RelativeLayout.LayoutParams
            lp.marginEnd = if (isLandscape) Utils.convertDp(this, 48f) else 0
        }

        // Adjust control margins
        run {
            val lp = binding.controls.layoutParams as RelativeLayout.LayoutParams

            lp.bottomMargin = if (!controlsAtBottom) {
                Utils.convertDp(this, 60f)
            } else {
                if (isLandscape || !hasSoftwareKeys) 0 else Utils.convertDp(this, 48f)
            }
            lp.leftMargin = if (!controlsAtBottom) {
                Utils.convertDp(this, if (isLandscape) 60f else 24f)
            } else {
                if (isLandscape && hasSoftwareKeys) Utils.convertDp(this, 48f) else 0
            }
            lp.rightMargin = lp.leftMargin
        }
    }

    override fun onPictureInPictureModeChanged(state: Boolean) {
        super.onPictureInPictureModeChanged(state)
        Log.v(TAG, "onPiPModeChanged($state)")
        if (state) {
            lockedUI = true
            hideControls()
            return
        }

        unlockUI(null)
        // For whatever stupid reason Android provides no good detection for when PiP is exited
        // so we have to do this shit (https://stackoverflow.com/questions/43174507/#answer-56127742)
        if (activityIsStopped) {
            // audio-only detection doesn't work in this situation, I don't care to fix this:
            this.backgroundPlayMode = "never"
            onPauseImpl() // behave as if the app normally went into background
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun playPause(view: View) = player.cyclePause()

    @Suppress("UNUSED_PARAMETER")
    fun playlistPrev(view: View) = MPVLib.command(arrayOf("playlist-prev"))

    @Suppress("UNUSED_PARAMETER")
    fun playlistNext(view: View) = MPVLib.command(arrayOf("playlist-next"))

    private fun showToast(msg: String) {
        toast.setText(msg)
        toast.show()
    }

    // Intent/Uri parsing

    private fun parsePathFromIntent(intent: Intent): String? {
        val filepath: String?
        filepath = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { resolveUri(it) }
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                val uri = Uri.parse(it.trim())
                if (uri.isHierarchical && !uri.isRelative) resolveUri(uri) else null
            }
            else -> intent.getStringExtra("filepath")
        }
        return filepath
    }

    private fun resolveUri(data: Uri): String? {
        val filepath = when (data.scheme) {
            "file" -> data.path
            "content" -> openContentFd(data)
            "http", "https", "rtmp", "rtmps", "rtp", "rtsp", "mms", "mmst", "mmsh", "tcp", "udp"
            -> data.toString()
            else -> null
        }

        if (filepath == null) {
            Log.e(TAG, "unknown scheme: ${data.scheme}")
        }
        return filepath
    }

    private fun openContentFd(uri: Uri): String? {
        val resolver = applicationContext.contentResolver
        Log.v(TAG, "Resolving content URI: $uri")
        val fd = try {
            val desc = resolver.openFileDescriptor(uri, "r")
            desc!!.detachFd()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open content fd: $e")
            return null
        }
        // Find out real file path and see if we can read it directly
        try {
            val path = File("/proc/self/fd/$fd").canonicalPath
            if (!path.startsWith("/proc") && File(path).canRead()) {
                Log.v(TAG, "Found real file path: $path")
                ParcelFileDescriptor.adoptFd(fd).close() // we don't need that anymore
                return path
            }
        } catch (e: Exception) { }
        // Else, pass the fd to mpv
        return "fdclose://$fd"
    }

    private fun parseIntentExtras(extras: Bundle?) {
        onloadCommands.clear()
        if (extras == null) {
            return
        }

        // API reference: http://mx.j2inter.com/api (partially implemented)
        if (extras.getByte("decode_mode") == 2.toByte()) {
            onloadCommands.add(arrayOf("set", "file-local-options/hwdec", "no"))
        }
        if (extras.containsKey("subs")) {
            val subList = extras.getParcelableArray("subs")?.mapNotNull { it as? Uri } ?: emptyList()
            val subsToEnable = extras.getParcelableArray("subs.enable")?.mapNotNull { it as? Uri } ?: emptyList()

            for (suburi in subList) {
                val subfile = resolveUri(suburi) ?: continue
                val flag = if (subsToEnable.filter { it.compareTo(suburi) == 0 }.any()) "select" else "auto"

                Log.v(TAG, "Adding subtitles from intent extras: $subfile")
                onloadCommands.add(arrayOf("sub-add", subfile, flag))
            }
        }
        if (extras.getInt("position", 0) > 0) {
            val pos = extras.getInt("position", 0) / 1000f
            onloadCommands.add(arrayOf("set", "start", pos.toString()))
        }
    }

    // UI (Part 2)

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
            showToast("$trackPrefix ${getString(R.string.track_off)}")
            return
        }

        val trackName = player.tracks[track_type]?.firstOrNull { it.mpvId == track_id }?.name ?: "???"
        showToast("$trackPrefix $trackName")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleAudio(view: View) = trackSwitchNotification {
        player.cycleAudio(); TrackData(player.aid, "audio")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cycleSub(view: View) = trackSwitchNotification {
        player.cycleSub(); TrackData(player.sid, "sub")
    }

    private fun selectTrack(type: String, get: () -> Int, set: (Int) -> Unit) {
        val tracks = player.tracks.getValue(type)
        val selectedMpvId = get()
        val selectedIndex = tracks.indexOfFirst { it.mpvId == selectedMpvId }
        val restore = pauseForDialog()

        with(AlertDialog.Builder(this)) {
            setSingleChoiceItems(tracks.map { it.name }.toTypedArray(), selectedIndex) { dialog, item ->
                val trackId = tracks[item].mpvId

                set(trackId)
                dialog.dismiss()
                trackSwitchNotification { TrackData(trackId, type) }
            }
            setOnDismissListener { restore() }
            create().show()
        }
    }

    private fun pickAudio() = selectTrack("audio", { player.aid }, { player.aid = it })

    private fun pickSub() = selectTrack("sub", { player.sid }, { player.sid = it })

    private fun openPlaylistMenu(restore: StateRestoreCallback) {
        val impl = PlaylistDialog(player)
        lateinit var dialog: AlertDialog

        impl.listeners = object : PlaylistDialog.Listeners {
            private fun openFilePicker(skip: Int) {
                openFilePickerFor(RCODE_LOAD_FILE, "", skip) { result, data ->
                    if (result == RESULT_OK) {
                        val path = data!!.getStringExtra("path")
                        MPVLib.command(arrayOf("loadfile", path, "append"))
                        impl.refresh()
                    }
                }
            }

            override fun pickFile() = openFilePicker(FilePickerActivity.FILE_PICKER)
            override fun openUrl() = openFilePicker(FilePickerActivity.URL_DIALOG)

            override fun onItemPicked(item: MPVView.PlaylistItem) {
                MPVLib.setPropertyInt("playlist-pos", item.index)
                dialog.dismiss()
            }
        }

        dialog = with(AlertDialog.Builder(this)) {
            setView(impl.buildView(layoutInflater))
            setOnDismissListener { restore() }
            create()
        }
        dialog.show()
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

    private fun pickSpeed() {
        // TODO: replace this with SliderPickerDialog
        val picker = SpeedPickerDialog()

        val restore = pauseForDialog()
        genericPickerDialog(picker, R.string.title_speed_dialog, "speed") {
            updateSpeedButton()
            restore()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun goIntoPiP(view: View?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        updatePiPParams()
        enterPictureInPictureMode()
    }

    @Suppress("UNUSED_PARAMETER")
    fun lockUI(view: View?) {
        lockedUI = true
        hideControlsDelayed()
    }

    @Suppress("UNUSED_PARAMETER")
    fun unlockUI(view: View?) {
        binding.unlockBtn.visibility = View.GONE
        lockedUI = false
        showControls()
    }

    data class MenuItem(@IdRes val idRes: Int, val handler: () -> Boolean)
    private fun genericMenu(
        @LayoutRes layoutRes: Int,
        buttons: List<MenuItem>,
        hiddenButtons: Set<Int>,
        restoreState: StateRestoreCallback
    ) {
        lateinit var dialog: AlertDialog
        val dialogView = layoutInflater.inflate(layoutRes, null)

        for (button in buttons) {
            val buttonView = dialogView.findViewById<Button>(button.idRes)
            buttonView.setOnClickListener {
                val ret = button.handler()
                if (ret) { // restore state immediately
                    restoreState()
                }
                dialog.dismiss()
            }
        }

        hiddenButtons.forEach { dialogView.findViewById<View>(it).visibility = View.GONE }

        if (Utils.visibleChildren(dialogView) == 0) {
            Log.w(TAG, "Not showing menu because it would be empty")
            restoreState()
            return
        }

        with(AlertDialog.Builder(this)) {
            setView(dialogView)
            setOnCancelListener { restoreState() }
            dialog = create()
        }
        dialog.show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun openTopMenu(view: View) {
        val restoreState = pauseForDialog()

        /******/
        val hiddenButtons = mutableSetOf<Int>()
        val buttons: MutableList<MenuItem> = mutableListOf(
            MenuItem(R.id.audioBtn) {
                openFilePickerFor(RCODE_EXTERNAL_AUDIO, R.string.open_external_audio) { result, data ->
                    if (result == RESULT_OK) {
                        MPVLib.command(arrayOf("audio-add", data!!.getStringExtra("path"), "cached"))
                    }
                    restoreState()
                }; false
            },
            MenuItem(R.id.subBtn) {
                openFilePickerFor(RCODE_EXTERNAL_SUB, R.string.open_external_sub) { result, data ->
                    if (result == RESULT_OK) {
                        MPVLib.command(arrayOf("sub-add", data!!.getStringExtra("path"), "cached"))
                    }
                    restoreState()
                }; false
            },
            MenuItem(R.id.playlistBtn) {
                openPlaylistMenu(restoreState); false
            },
            MenuItem(R.id.backgroundBtn) {
                backgroundPlayMode = "always"
                player.paused = false
                moveTaskToBack(true)
                false
            },
            MenuItem(R.id.advancedBtn) { openAdvancedMenu(restoreState); false },
            MenuItem(R.id.statsBtn) {
                MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle")); true
            },
            MenuItem(R.id.orientationBtn) { this.cycleOrientation(); true }
        )

        val statsButtons = arrayOf(R.id.statsBtn1, R.id.statsBtn2, R.id.statsBtn3)
        for (i in 1..3) {
            buttons.add(
                MenuItem(statsButtons[i - 1]) {
                    MPVLib.command(arrayOf("script-binding", "stats/display-page-$i")); true
                }
            )
        }

        if (player.aid == -1) {
            hiddenButtons.add(R.id.backgroundBtn)
        }
        if (autoRotationMode == "auto") {
            hiddenButtons.add(R.id.orientationBtn)
        }
        /******/

        genericMenu(R.layout.dialog_top_menu, buttons, hiddenButtons, restoreState)
    }

    private fun genericPickerDialog(
        picker: PickerDialog,
        @StringRes titleRes: Int,
        property: String,
        restoreState: StateRestoreCallback
    ) {
        val dialog = with(AlertDialog.Builder(this)) {
            setTitle(titleRes)
            setView(picker.buildView(layoutInflater))
            setPositiveButton(R.string.dialog_ok) { _, _ ->
                picker.number?.let {
                    if (picker.isInteger()) {
                        MPVLib.setPropertyInt(property, it.toInt())
                    } else {
                        MPVLib.setPropertyDouble(property, it)
                    }
                }
            }
            setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
            setOnDismissListener { restoreState() }
            create()
        }

        picker.number = MPVLib.getPropertyDouble(property)
        dialog.show()
    }

    private fun openAdvancedMenu(restoreState: StateRestoreCallback) {
        /******/
        val hiddenButtons = mutableSetOf<Int>()
        val buttons: MutableList<MenuItem> = mutableListOf(
            MenuItem(R.id.subSeekPrev) {
                MPVLib.command(arrayOf("sub-seek", "-1")); true
            },
            MenuItem(R.id.subSeekNext) {
                MPVLib.command(arrayOf("sub-seek", "1")); true
            },
            MenuItem(R.id.chapterBtn) {
                val chapters = player.loadChapters()
                if (chapters.isEmpty()) {
                    return@MenuItem true
                }
                val chapterArray = chapters.map {
                    val timecode = Utils.prettyTime(it.time.roundToInt())
                    if (!it.title.isNullOrEmpty()) {
                        getString(R.string.ui_chapter, it.title, timecode)
                    } else {
                        getString(R.string.ui_chapter_fallback, it.index + 1, timecode)
                    }
                }.toTypedArray()
                val selectedIndex = MPVLib.getPropertyInt("chapter") ?: 0
                with(AlertDialog.Builder(this)) {
                    setSingleChoiceItems(chapterArray, selectedIndex) { dialog, item ->
                        MPVLib.setPropertyInt("chapter", chapters[item].index)
                        dialog.dismiss()
                    }
                    setOnDismissListener { restoreState() }
                    create().show()
                }; false
            },
            MenuItem(R.id.chapterPrev) {
                MPVLib.command(arrayOf("add", "chapter", "-1")); true
            },
            MenuItem(R.id.chapterNext) {
                MPVLib.command(arrayOf("add", "chapter", "1")); true
            },
            MenuItem(R.id.aspectBtn) {
                val ratios = resources.getStringArray(R.array.aspect_ratios)
                with(AlertDialog.Builder(this)) {
                    setItems(R.array.aspect_ratio_names) { dialog, item ->
                        MPVLib.command(arrayOf("set", "video-aspect-override", ratios[item]))
                        dialog.dismiss()
                    }
                    setOnDismissListener { restoreState() }
                    create().show()
                }; false
            },
        )

        // contrast, brightness and others get a -100 to 100 slider
        val basicIds = arrayOf(R.id.contrastBtn, R.id.brightnessBtn, R.id.gammaBtn, R.id.saturationBtn)
        val basicProps = arrayOf("contrast", "brightness", "gamma", "saturation")
        val basicTitles = arrayOf(R.string.contrast, R.string.video_brightness, R.string.gamma, R.string.saturation)
        basicIds.forEachIndexed { index, id ->
            buttons.add(
                MenuItem(id) {
                    val slider = SliderPickerDialog(-100.0, 100.0, 1, R.string.format_fixed_number)
                    genericPickerDialog(slider, basicTitles[index], basicProps[index], restoreState)
                    false
                }
            )
        }

        // audio / sub delay get a decimal picker
        arrayOf(R.id.audioDelayBtn, R.id.subDelayBtn).forEach { id ->
            val title = if (id == R.id.audioDelayBtn) R.string.audio_delay else R.string.sub_delay
            val prop = if (id == R.id.audioDelayBtn) "audio-delay" else "sub-delay"
            buttons.add(
                MenuItem(id) {
                    val picker = DecimalPickerDialog(-600.0, 600.0)
                    genericPickerDialog(picker, title, prop, restoreState)
                    false
                }
            )
        }

        if (player.vid == -1) {
            hiddenButtons.addAll(arrayOf(R.id.rowVideo1, R.id.rowVideo2, R.id.aspectBtn))
        }
        if (player.aid == -1 || player.vid == -1) {
            hiddenButtons.add(R.id.audioDelayBtn)
        }
        if (player.sid == -1) {
            hiddenButtons.addAll(arrayOf(R.id.subDelayBtn, R.id.rowSubSeek))
        }
        if (MPVLib.getPropertyInt("chapter-list/count") ?: 0 == 0) {
            hiddenButtons.add(R.id.rowChapter)
        }
        /******/

        genericMenu(R.layout.dialog_advanced_menu, buttons, hiddenButtons, restoreState)
    }

    private fun cycleOrientation() {
        requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private var activityResultCallbacks: MutableMap<Int, ActivityResultCallback> = mutableMapOf()
    private fun openFilePickerFor(requestCode: Int, title: String, skip: Int?, callback: ActivityResultCallback) {
        val intent = Intent(this, FilePickerActivity::class.java)
        intent.putExtra("title", title)
        skip?.let { intent.putExtra("skip", it) }
        // start file picker at directory of current file
        val path = MPVLib.getPropertyString("path") ?: ""
        if (path.startsWith('/')) {
            intent.putExtra("default_path", File(path).parent)
        }

        activityResultCallbacks[requestCode] = callback
        startActivityForResult(intent, requestCode)
    }
    private fun openFilePickerFor(requestCode: Int, @StringRes titleRes: Int, callback: ActivityResultCallback) {
        openFilePickerFor(requestCode, getString(titleRes), null, callback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityResultCallbacks.remove(requestCode)?.invoke(resultCode, data)
    }

    private fun refreshUi() {
        // forces update of entire UI, used when resuming the activity
        val paused = player.paused ?: return
        updatePlaybackStatus(paused)
        player.timePos?.let { updatePlaybackPos(it) }
        player.duration?.let { updatePlaybackDuration(it) }
        updateAudioUI()
        if (useAudioUI || showMediaTitle) {
            updateDisplayMetadata("", "")
        }
        updatePlaylistButtons()
        player.loadTracks()
    }

    private fun updateAudioUI() {
        val audioButtons = arrayOf(
            R.id.prevBtn, R.id.cycleAudioBtn, R.id.playBtn,
            R.id.cycleSpeedBtn, R.id.nextBtn
        )
        val videoButtons = arrayOf(
            R.id.playBtn, R.id.cycleAudioBtn, R.id.cycleSubsBtn,
            R.id.cycleDecoderBtn, R.id.cycleSpeedBtn
        )

        val shouldUseAudioUI = isPlayingAudioOnly()
        if (shouldUseAudioUI == useAudioUI) {
            return
        }
        useAudioUI = shouldUseAudioUI
        Log.v(TAG, "Audio UI: $useAudioUI")

        val seekbarGroup = binding.controlsSeekbarGroup
        val buttonGroup = binding.controlsButtonGroup

        if (useAudioUI) {
            // Move prev/next file from seekbar group to buttons group
            Utils.viewGroupMove(seekbarGroup, R.id.prevBtn, buttonGroup, 0)
            Utils.viewGroupMove(seekbarGroup, R.id.nextBtn, buttonGroup, -1)

            // Change button layout of buttons group
            Utils.viewGroupReorder(buttonGroup, audioButtons)

            // Show song title and more metadata
            binding.controlsTitleGroup.visibility = View.VISIBLE
            Utils.viewGroupReorder(binding.controlsTitleGroup, arrayOf(R.id.titleTextView, R.id.minorTitleTextView))
            updateDisplayMetadata("", "")

            showControls()
        } else {
            Utils.viewGroupMove(buttonGroup, R.id.prevBtn, seekbarGroup, 0)
            Utils.viewGroupMove(buttonGroup, R.id.nextBtn, seekbarGroup, -1)

            Utils.viewGroupReorder(buttonGroup, videoButtons)

            // Show title only depending on settings
            if (showMediaTitle) {
                binding.controlsTitleGroup.visibility = View.VISIBLE
                Utils.viewGroupReorder(binding.controlsTitleGroup, arrayOf(R.id.fullTitleTextView))
                updateDisplayMetadata("", "")
            } else {
                binding.controlsTitleGroup.visibility = View.GONE
            }

            hideControls() // do NOT use fade runnable
        }

        // Visibility might have changed, so update
        updatePlaylistButtons()
    }

    private var cachedMeta = Utils.AudioMetadata()

    private fun updateDisplayMetadata(property: String, value: String) {
        if (property.isEmpty()) {
            cachedMeta.readAll()
        } else {
            if (!cachedMeta.update(property, value)) {
                return
            }
        }

        if (!useAudioUI) {
            if (showMediaTitle) {
                binding.fullTitleTextView.text = cachedMeta.formatTitle()
            }
            return
        }
        binding.titleTextView.text = cachedMeta.formatTitle()
        binding.minorTitleTextView.text = cachedMeta.formatArtistAlbum()
    }

    fun updatePlaybackPos(position: Int) {
        binding.playbackPositionTxt.text = Utils.prettyTime(position)
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

    private fun updatePlaybackStatus(paused: Boolean) {
        val r = if (paused) R.drawable.ic_play_arrow_black_24dp else R.drawable.ic_pause_black_24dp
        binding.playBtn.setImageResource(r)

        if (lockedUI) {
            updatePiPParams()
        }
        if (paused) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    private fun updatePlaylistButtons() {
        val plCount = MPVLib.getPropertyInt("playlist-count") ?: 1
        val plPos = MPVLib.getPropertyInt("playlist-pos") ?: 0

        if (!useAudioUI && plCount == 1) {
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

    private fun updateOrientation(initial: Boolean = false) {
        if (autoRotationMode != "auto") {
            if (!initial) {
                return // don't reset at runtime
            }
            requestedOrientation = when (autoRotationMode) {
                "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        if (initial || player.vid == -1) {
            return
        }

        val ratio = player.videoAspect?.toFloat() ?: 0f
        Log.v(TAG, "auto rotation: aspect ratio = $ratio")

        if (ratio == 0f || ratio in (1f / ASPECT_RATIO_MIN)..ASPECT_RATIO_MIN) {
            // video is square, let Android do what it wants
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            return
        }
        requestedOrientation = if (ratio > 1f) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
    }

    private fun updatePiPParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val intent1 = NotificationButtonReceiver.createIntent(this, "PLAY_PAUSE")
        val action1 = if (player.paused ?: true) {
            RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_play_arrow_black_24dp),
                "Play", "", intent1
            )
        } else {
            RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_pause_black_24dp),
                "Pause", "", intent1
            )
        }

        val params = with(PictureInPictureParams.Builder()) {
            val aspect = player.videoAspect ?: 1.0
            setAspectRatio(Rational(aspect.times(10000).toInt(), 10000))
            setActions(listOf(action1))
        }
        try {
            setPictureInPictureParams(params.build())
        } catch (e: IllegalArgumentException) {
            // Android has some limits of what the aspect ratio can be
            params.setAspectRatio(Rational(1, 1))
            setPictureInPictureParams(params.build())
        }
    }

    // mpv events

    private fun eventPropertyUi(property: String) {
        if (!activityIsForeground) return
        when (property) {
            "track-list" -> player.loadTracks()
            "video-params" -> updateOrientation()
            "playlist-pos", "playlist-count" -> updatePlaylistButtons()
            "video-format" -> updateAudioUI()
        }
    }

    private fun eventPropertyUi(property: String, value: Boolean) {
        if (!activityIsForeground) return
        when (property) {
            "pause" -> updatePlaybackStatus(value)
        }
    }

    private fun eventPropertyUi(property: String, value: Long) {
        if (!activityIsForeground) return
        when (property) {
            "time-pos" -> updatePlaybackPos(value.toInt())
            "duration" -> updatePlaybackDuration(value.toInt())
        }
    }

    private fun eventPropertyUi(property: String, value: String) {
        if (!activityIsForeground) return
        updateDisplayMetadata(property, value)
    }

    private fun eventUi(eventId: Int) {
        when (eventId) {
            MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> updatePlaybackStatus(player.paused!!)
        }
    }

    override fun eventProperty(property: String) {
        if (!activityIsForeground) return
        runOnUiThread { eventPropertyUi(property) }
    }

    override fun eventProperty(property: String, value: Boolean) {
        if (!activityIsForeground) return
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: Long) {
        if (!activityIsForeground) return
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun eventProperty(property: String, value: String) {
        if (!activityIsForeground) return
        runOnUiThread { eventPropertyUi(property, value) }
    }

    override fun event(eventId: Int) {
        // finish on idle/shutdown
        if (playbackHasStarted && eventId == MPVLib.mpvEventId.MPV_EVENT_IDLE) {
            finishWithResult(RESULT_OK)
        } else if (eventId == MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN) {
            finishWithResult(if (playbackHasStarted) RESULT_OK else RESULT_CANCELED)
        }

        if (eventId == MPVLib.mpvEventId.MPV_EVENT_START_FILE) {
            for (c in onloadCommands)
                MPVLib.command(c)
            if (this.statsLuaMode > 0 && !playbackHasStarted) {
                MPVLib.command(arrayOf("script-binding", "stats/display-stats-toggle"))
                MPVLib.command(arrayOf("script-binding", "stats/${this.statsLuaMode}"))
            }

            playbackHasStarted = true
        }

        if (!activityIsForeground) return

        runOnUiThread { eventUi(eventId) }
    }

    // Gesture handler

    private var initialSeek = 0
    private var initialBright = 0f
    private var initialVolume = 0
    private var maxVolume = 0

    private fun fadeGestureText() {
        fadeHandler.removeCallbacks(fadeRunnable3)
        binding.gestureTextView.visibility = View.VISIBLE

        fadeHandler.postDelayed(fadeRunnable3, 500L)
    }

    override fun onPropertyChange(p: PropertyChange, diff: Float) {
        val gestureTextView = binding.gestureTextView
        when (p) {
            /* Drag gestures */
            PropertyChange.Init -> {
                mightWantToToggleControls = false

                initialSeek = player.timePos ?: -1
                initialBright = Utils.getScreenBrightness(this) ?: 0.5f
                initialVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
                maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

                fadeHandler.removeCallbacks(fadeRunnable3)
                gestureTextView.visibility = View.VISIBLE
                gestureTextView.text = ""
            }
            PropertyChange.Seek -> {
                // disable seeking when timePos is not available
                val duration = player.duration ?: 0
                if (duration == 0 || initialSeek < 0) {
                    return
                }
                val newPos = (initialSeek + diff.toInt()).coerceIn(0, duration)
                val newDiff = newPos - initialSeek
                // seek faster than assigning to timePos but less precise
                MPVLib.command(arrayOf("seek", newPos.toString(), "absolute+keyframes"))
                updatePlaybackPos(newPos)

                val diffText = Utils.prettyTime(newDiff, true)
                gestureTextView.text = getString(R.string.ui_seek_distance, Utils.prettyTime(newPos), diffText)
            }
            PropertyChange.Volume -> {
                val newVolume = (initialVolume + (diff * maxVolume).toInt()).coerceIn(0, maxVolume)
                val newVolumePercent = 100 * newVolume / maxVolume
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)

                gestureTextView.text = getString(R.string.ui_volume, newVolumePercent)
            }
            PropertyChange.Bright -> {
                val lp = window.attributes
                val newBright = (initialBright + diff).coerceIn(0f, 1f)
                lp.screenBrightness = newBright
                window.attributes = lp

                gestureTextView.text = getString(R.string.ui_brightness, (newBright * 100).roundToInt())
            }
            PropertyChange.Finalize -> gestureTextView.visibility = View.GONE

            /* Tap gestures */
            PropertyChange.SeekFixed -> {
                val seekTime = diff * 10f
                val newPos = (player.timePos ?: 0) + seekTime.toInt() // only for display
                MPVLib.command(arrayOf("seek", seekTime.toString(), "relative"))

                val diffText = Utils.prettyTime(seekTime.toInt(), true)
                gestureTextView.text = getString(R.string.ui_seek_distance, Utils.prettyTime(newPos), diffText)
                fadeGestureText()
            }
            PropertyChange.PlayPause -> player.cyclePause()
            PropertyChange.PanScan -> MPVLib.command(arrayOf("cycle-values", "panscan", "1.0", "0.0"))
            PropertyChange.Custom -> {
                val keycode = 0x10002 + diff.toInt()
                MPVLib.command(arrayOf("keypress", "0x%x".format(keycode)))
            }
        }
    }

    companion object {
        private const val TAG = "mpv"

        // how long should controls be displayed on screen (ms)
        private const val CONTROLS_DISPLAY_TIMEOUT = 1500L

        // how long controls fade to disappear (ms)
        private const val CONTROLS_FADE_DURATION = 500L

        // size (px) of the thumbnail displayed with background play notification
        private const val THUMB_SIZE = 192

        // smallest aspect ratio that is considered non-square
        private const val ASPECT_RATIO_MIN = 1.2f // covers 5:4 and up

        // fraction to which audio volume is ducked on loss of audio focus
        private const val AUDIO_FOCUS_DUCKING = 0.5f

        // request codes for invoking other activities
        private const val RCODE_EXTERNAL_AUDIO = 1000
        private const val RCODE_EXTERNAL_SUB = 1001
        private const val RCODE_LOAD_FILE = 1002

        // action of result intent
        private const val RESULT_INTENT = "is.xyz.mpv.MPVActivity.result"
    }
}
