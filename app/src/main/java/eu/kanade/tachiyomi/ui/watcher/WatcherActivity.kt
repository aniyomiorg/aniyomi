package eu.kanade.tachiyomi.ui.watcher

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.ProgressDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.data.preference.toggle
import eu.kanade.tachiyomi.databinding.WatcherActivityBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.watcher.WatcherPresenter.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.watcher.WatcherPresenter.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.watcher.WatcherPresenter.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.watcher.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.ui.watcher.setting.OrientationType
import eu.kanade.tachiyomi.ui.watcher.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.watcher.setting.WatcherSettingsSheet
import eu.kanade.tachiyomi.ui.watcher.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.watcher.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.watcher.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.watcher.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.watcher.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.defaultBar
import eu.kanade.tachiyomi.util.view.hideBar
import eu.kanade.tachiyomi.util.view.isDefaultBar
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.util.view.setTooltip
import eu.kanade.tachiyomi.util.view.showBar
import eu.kanade.tachiyomi.widget.SimpleAnimationListener
import eu.kanade.tachiyomi.widget.SimpleSeekBarListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import nucleus.factory.RequiresPresenter
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File
import kotlin.math.abs

/**
 * Activity containing the watcher of Tachiyomi. This activity is mostly a container of the
 * viewers, to which calls from the presenter or UI events are delegated.
 */
@RequiresPresenter(WatcherPresenter::class)
class WatcherActivity : BaseRxActivity<WatcherActivityBinding, WatcherPresenter>() {

    companion object {
        fun newIntent(context: Context, anime: Anime, episode: Episode): Intent {
            return Intent(context, WatcherActivity::class.java).apply {
                putExtra("anime", anime.id)
                putExtra("episode", episode.id)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    private val preferences: PreferencesHelper by injectLazy()

    /**
     * The maximum bitmap size supported by the device.
     */
    val maxBitmapSize by lazy { GLUtil.maxTextureSize }

    val hasCutout by lazy { hasDisplayCutout() }

    /**
     * Viewer used to display the pages (pager, webtoon, ...).
     */
    var viewer: BaseViewer? = null
        private set

    /**
     * Whether the menu is currently visible.
     */
    var menuVisible = false
        private set

    /**
     * Configuration at watcher level, like background color or forced orientation.
     */
    private var config: WatcherConfig? = null

    /**
     * Progress dialog used when switching episodes from the menu buttons.
     */
    @Suppress("DEPRECATION")
    private var progressDialog: ProgressDialog? = null

    private var menuToggleToast: Toast? = null

    private var readingModeToast: Toast? = null

    /**
     * Called when the activity is created. Initializes the presenter and configuration.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(
            when (preferences.watcherTheme().get()) {
                0 -> R.style.Theme_Reader_Light
                2 -> R.style.Theme_Reader_Dark_Grey
                else -> R.style.Theme_Reader_Dark
            }
        )
        super.onCreate(savedInstanceState)

        binding = WatcherActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (presenter.needsInit()) {
            val anime = intent.extras!!.getLong("anime", -1)
            val episode = intent.extras!!.getLong("episode", -1)
            if (anime == -1L || episode == -1L) {
                finish()
                return
            }
            NotificationReceiver.dismissNotification(this, anime.hashCode(), Notifications.ID_NEW_CHAPTERS)
            presenter.init(anime, episode)
        }

        if (savedInstanceState != null) {
            menuVisible = savedInstanceState.getBoolean(::menuVisible.name)
        }

        config = WatcherConfig()
        initializeMenu()

        // Avoid status bar showing up on rotation
        window.decorView.setOnSystemUiVisibilityChangeListener {
            setMenuVisibility(menuVisible, animate = false)
        }
    }

    /**
     * Called when the activity is destroyed. Cleans up the viewer, configuration and any view.
     */
    override fun onDestroy() {
        super.onDestroy()
        viewer?.destroy()
        viewer = null
        config = null
        menuToggleToast?.cancel()
        readingModeToast?.cancel()
        progressDialog?.dismiss()
        progressDialog = null
    }

    /**
     * Called when the activity is saving instance state. Current progress is persisted if this
     * activity isn't changing configurations.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(::menuVisible.name, menuVisible)
        if (!isChangingConfigurations) {
            presenter.onSaveInstanceStateNonConfigurationChange()
        }
        super.onSaveInstanceState(outState)
    }

    /**
     * Set menu visibility again on activity resume to apply immersive mode again if needed.
     * Helps with rotations.
     */
    override fun onResume() {
        super.onResume()
        setMenuVisibility(menuVisible, animate = false)
    }

    /**
     * Called when the window focus changes. It sets the menu visibility to the last known state
     * to apply immersive mode again if needed.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(menuVisible, animate = false)
        }
    }

    /**
     * Called when the options menu of the toolbar is being created. It adds our custom menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.watcher, menu)

        val isEpisodeBookmarked = presenter?.getCurrentEpisode()?.episode?.bookmark ?: false
        menu.findItem(R.id.action_bookmark).isVisible = !isEpisodeBookmarked
        menu.findItem(R.id.action_remove_bookmark).isVisible = isEpisodeBookmarked

        return true
    }

    /**
     * Called when an item of the options menu was clicked. Used to handle clicks on our menu
     * entries.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_bookmark -> {
                presenter.bookmarkCurrentEpisode(true)
                invalidateOptionsMenu()
            }
            R.id.action_remove_bookmark -> {
                presenter.bookmarkCurrentEpisode(false)
                invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Called when the user clicks the back key or the button on the toolbar. The call is
     * delegated to the presenter.
     */
    override fun onBackPressed() {
        presenter.onBackPressed()
        super.onBackPressed()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_N) {
            presenter.loadNextEpisode()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_P) {
            presenter.loadPreviousEpisode()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * Dispatches a key event. If the viewer doesn't handle it, call the default implementation.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewer?.handleKeyEvent(event) ?: false
        return handled || super.dispatchKeyEvent(event)
    }

    /**
     * Dispatches a generic motion event. If the viewer doesn't handle it, call the default
     * implementation.
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = viewer?.handleGenericMotionEvent(event) ?: false
        return handled || super.dispatchGenericMotionEvent(event)
    }

    /**
     * Initializes the watcher menu. It sets up click listeners and the initial visibility.
     */
    private fun initializeMenu() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.watcherMenu) { _, insets ->
            if (!window.isDefaultBar()) {
                val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.watcherMenu.setPadding(
                    systemInsets.left,
                    systemInsets.top,
                    systemInsets.right,
                    systemInsets.bottom
                )
            }
            insets
        }

        binding.toolbar.setOnClickListener {
            presenter.anime?.id?.let { id ->
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        action = MainActivity.SHORTCUT_MANGA
                        putExtra(AnimeController.MANGA_EXTRA, id)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            }
        }

        // Init listeners on bottom menu
        binding.pageSeekbar.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (viewer != null && fromUser) {
                        moveToPageIndex(value)
                    }
                }
            }
        )
        binding.leftEpisode.setOnClickListener {
            if (viewer != null) {
                if (viewer is R2LPagerViewer) {
                    loadNextEpisode()
                } else {
                    loadPreviousEpisode()
                }
            }
        }
        binding.rightEpisode.setOnClickListener {
            if (viewer != null) {
                if (viewer is R2LPagerViewer) {
                    loadPreviousEpisode()
                } else {
                    loadNextEpisode()
                }
            }
        }

        initBottomShortcuts()

        // Set initial visibility
        setMenuVisibility(menuVisible)
    }

    private fun initBottomShortcuts() {
        // Reading mode
        with(binding.actionReadingMode) {
            setTooltip(R.string.viewer)

            setOnClickListener {
                popupMenu(
                    items = ReadingModeType.values().map { it.prefValue to it.stringRes },
                    selectedItemId = presenter.getAnimeViewer(resolveDefault = false),
                ) {
                    val newReadingMode = ReadingModeType.fromPreference(itemId)

                    presenter.setAnimeViewer(newReadingMode.prefValue)

                    menuToggleToast?.cancel()
                    if (!preferences.showReadingMode()) {
                        menuToggleToast = toast(newReadingMode.stringRes)
                    }
                }
            }
        }

        // Rotation
        with(binding.actionRotation) {
            setTooltip(R.string.pref_rotation_type)

            setOnClickListener {
                popupMenu(
                    items = OrientationType.values().map { it.prefValue to it.stringRes },
                    selectedItemId = preferences.rotation().get(),
                ) {
                    val newOrientation = OrientationType.fromPreference(itemId)

                    preferences.rotation().set(newOrientation.prefValue)
                    setOrientation(newOrientation.flag)

                    menuToggleToast?.cancel()
                    menuToggleToast = toast(newOrientation.stringRes)
                }
            }
        }
        preferences.rotation().asImmediateFlow { updateRotationShortcut(it) }
            .launchIn(lifecycleScope)

        // Crop borders
        with(binding.actionCropBorders) {
            setTooltip(R.string.pref_crop_borders)

            setOnClickListener {
                val isPagerType = ReadingModeType.isPagerType(presenter.getAnimeViewer())
                if (isPagerType) {
                    preferences.cropBorders().toggle()
                } else {
                    preferences.cropBordersWebtoon().toggle()
                }
            }
        }
        updateCropBordersShortcut()
        listOf(preferences.cropBorders(), preferences.cropBordersWebtoon())
            .forEach { pref ->
                pref.asFlow()
                    .onEach { updateCropBordersShortcut() }
                    .launchIn(lifecycleScope)
            }

        // Settings sheet
        with(binding.actionSettings) {
            setTooltip(R.string.action_settings)

            setOnClickListener {
                WatcherSettingsSheet(this@WatcherActivity).show()
            }

            setOnLongClickListener {
                WatcherSettingsSheet(this@WatcherActivity, showColorFilterSettings = true).show()
                true
            }
        }
    }

    private fun updateRotationShortcut(preference: Int) {
        val orientation = OrientationType.fromPreference(preference)
        binding.actionRotation.setImageResource(orientation.iconRes)
    }

    private fun updateCropBordersShortcut() {
        val isPagerType = ReadingModeType.isPagerType(presenter.getAnimeViewer())
        val enabled = if (isPagerType) {
            preferences.cropBorders().get()
        } else {
            preferences.cropBordersWebtoon().get()
        }

        binding.actionCropBorders.setImageResource(
            if (enabled) {
                R.drawable.ic_crop_24dp
            } else {
                R.drawable.ic_crop_off_24dp
            }
        )
    }

    /**
     * Sets the visibility of the menu according to [visible] and with an optional parameter to
     * [animate] the views.
     */
    fun setMenuVisibility(visible: Boolean, animate: Boolean = true) {
        menuVisible = visible
        if (visible) {
            if (preferences.fullscreen().get()) {
                window.showBar()
            } else {
                resetDefaultMenuAndBar()
            }
            binding.watcherMenu.isVisible = true

            if (animate) {
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_top)
                toolbarAnimation.setAnimationListener(
                    object : SimpleAnimationListener() {
                        override fun onAnimationStart(animation: Animation) {
                            // Fix status bar being translucent the first time it's opened.
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        }
                    }
                )
                binding.toolbar.startAnimation(toolbarAnimation)

                val bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom)
                binding.watcherMenuBottom.startAnimation(bottomAnimation)
            }

            if (preferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(false)
            }
        } else {
            if (preferences.fullscreen().get()) {
                window.hideBar()
            } else {
                resetDefaultMenuAndBar()
            }

            if (animate) {
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_top)
                toolbarAnimation.setAnimationListener(
                    object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animation) {
                            binding.watcherMenu.isVisible = false
                        }
                    }
                )
                binding.toolbar.startAnimation(toolbarAnimation)

                val bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_bottom)
                binding.watcherMenuBottom.startAnimation(bottomAnimation)
            }

            if (preferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(true)
            }
        }
    }

    /**
     * Reset menu padding and system bar
     */
    private fun resetDefaultMenuAndBar() {
        binding.watcherMenu.setPadding(0)
        window.defaultBar()
    }

    /**
     * Called from the presenter when a anime is ready. Used to instantiate the appropriate viewer
     * and the toolbar title.
     */
    fun setAnime(anime: Anime) {
        val prevViewer = viewer

        val viewerMode = ReadingModeType.fromPreference(presenter.getAnimeViewer(resolveDefault = false))
        binding.actionReadingMode.setImageResource(viewerMode.iconRes)

        val newViewer = when (presenter.getAnimeViewer()) {
            ReadingModeType.LEFT_TO_RIGHT.prefValue -> L2RPagerViewer(this)
            ReadingModeType.VERTICAL.prefValue -> VerticalPagerViewer(this)
            ReadingModeType.WEBTOON.prefValue -> WebtoonViewer(this)
            ReadingModeType.CONTINUOUS_VERTICAL.prefValue -> WebtoonViewer(this, isContinuous = false)
            else -> R2LPagerViewer(this)
        }

        // Destroy previous viewer if there was one
        if (prevViewer != null) {
            prevViewer.destroy()
            binding.viewerContainer.removeAllViews()
        }
        viewer = newViewer
        binding.viewerContainer.addView(newViewer.getView())

        if (preferences.showReadingMode()) {
            showReadingModeToast(presenter.getAnimeViewer())
        }

        binding.toolbar.title = anime.title

        binding.pageSeekbar.isRTL = newViewer is R2LPagerViewer
        if (newViewer is R2LPagerViewer) {
            binding.leftEpisode.setTooltip(R.string.action_next_episode)
            binding.rightEpisode.setTooltip(R.string.action_previous_episode)
        } else {
            binding.leftEpisode.setTooltip(R.string.action_previous_episode)
            binding.rightEpisode.setTooltip(R.string.action_next_episode)
        }

        binding.pleaseWait.isVisible = true
        binding.pleaseWait.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_long))
    }

    private fun showReadingModeToast(mode: Int) {
        try {
            val strings = resources.getStringArray(R.array.viewers_selector)
            readingModeToast?.cancel()
            readingModeToast = toast(strings[mode])
        } catch (e: ArrayIndexOutOfBoundsException) {
            Timber.e("Unknown reading mode: $mode")
        }
    }

    /**
     * Called from the presenter whenever a new [viewerEpisodes] have been set. It delegates the
     * method to the current viewer, but also set the subtitle on the toolbar.
     */
    fun setEpisodes(viewerEpisodes: ViewerEpisodes) {
        binding.pleaseWait.isVisible = false
        viewer?.setEpisodes(viewerEpisodes)
        binding.toolbar.subtitle = viewerEpisodes.currEpisode.episode.name

        // Invalidate menu to show proper episode bookmark state
        invalidateOptionsMenu()
    }

    /**
     * Called from the presenter if the initial load couldn't load the pages of the episode. In
     * this case the activity is closed and a toast is shown to the user.
     */
    fun setInitialEpisodeError(error: Throwable) {
        Timber.e(error)
        finish()
        toast(error.message)
    }

    /**
     * Called from the presenter whenever it's loading the next or previous episode. It shows or
     * dismisses a non-cancellable dialog to prevent user interaction according to the value of
     * [show]. This is only used when the next/previous buttons on the toolbar are clicked; the
     * other cases are handled with episode transitions on the viewers and episode preloading.
     */
    @Suppress("DEPRECATION")
    fun setProgressDialog(show: Boolean) {
        progressDialog?.dismiss()
        progressDialog = if (show) {
            ProgressDialog.show(this, null, getString(R.string.loading), true)
        } else {
            null
        }
    }

    /**
     * Moves the viewer to the given page [index]. It does nothing if the viewer is null or the
     * page is not found.
     */
    fun moveToPageIndex(index: Int) {
        val viewer = viewer ?: return
        val currentEpisode = presenter.getCurrentEpisode() ?: return
        val page = currentEpisode.seconds?.getOrNull(index) ?: return
        viewer.moveToPage(page)
    }

    /**
     * Tells the presenter to load the next episode and mark it as active. The progress dialog
     * should be automatically shown.
     */
    private fun loadNextEpisode() {
        presenter.loadNextEpisode()
    }

    /**
     * Tells the presenter to load the previous episode and mark it as active. The progress dialog
     * should be automatically shown.
     */
    private fun loadPreviousEpisode() {
        presenter.loadPreviousEpisode()
    }

    /**
     * Called from the viewer whenever a [page] is marked as active. It updates the values of the
     * bottom menu and delegates the change to the presenter.
     */
    @SuppressLint("SetTextI18n")
    fun onPageSelected(page: WatcherPage) {
        presenter.onPageSelected(page)
        val pages = page.episode.seconds ?: return

        // Set bottom page number
        binding.pageNumber.text = "${page.number}/${pages.size}"

        // Set seekbar page number
        if (viewer !is R2LPagerViewer) {
            binding.leftPageText.text = "${page.number}"
            binding.rightPageText.text = "${pages.size}"
        } else {
            binding.rightPageText.text = "${page.number}"
            binding.leftPageText.text = "${pages.size}"
        }

        // Set seekbar progress
        binding.pageSeekbar.max = pages.lastIndex
        binding.pageSeekbar.progress = page.index
    }

    /**
     * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
     * actions to perform is shown.
     */
    fun onPageLongTap(page: WatcherPage) {
        WatcherPageSheet(this, page).show()
    }

    /**
     * Called from the viewer when the given [episode] should be preloaded. It should be called when
     * the viewer is reaching the beginning or end of a episode or the transition page is active.
     */
    fun requestPreloadEpisode(episode: WatcherEpisode) {
        presenter.preloadEpisode(episode)
    }

    /**
     * Called from the viewer to toggle the visibility of the menu. It's implemented on the
     * viewer because each one implements its own touch and key events.
     */
    fun toggleMenu() {
        setMenuVisibility(!menuVisible)
    }

    /**
     * Called from the viewer to show the menu.
     */
    fun showMenu() {
        if (!menuVisible) {
            setMenuVisibility(true)
        }
    }

    /**
     * Called from the page sheet. It delegates the call to the presenter to do some IO, which
     * will call [onShareImageResult] with the path the image was saved on when it's ready.
     */
    fun shareImage(page: WatcherPage) {
        presenter.shareImage(page)
    }

    /**
     * Called from the presenter when a page is ready to be shared. It shows Android's default
     * sharing tool.
     */
    fun onShareImageResult(file: File, page: WatcherPage) {
        val anime = presenter.anime ?: return
        val episode = page.episode.episode

        val uri = file.getUriCompat(this)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_page_info, anime.title, episode.name, page.number))
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/*"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    /**
     * Called from the page sheet. It delegates saving the image of the given [page] on external
     * storage to the presenter.
     */
    fun saveImage(page: WatcherPage) {
        presenter.saveImage(page)
    }

    /**
     * Called from the presenter when a page is saved or fails. It shows a message or logs the
     * event depending on the [result].
     */
    fun onSaveImageResult(result: WatcherPresenter.SaveImageResult) {
        when (result) {
            is WatcherPresenter.SaveImageResult.Success -> {
                toast(R.string.picture_saved)
            }
            is WatcherPresenter.SaveImageResult.Error -> {
                Timber.e(result.error)
            }
        }
    }

    /**
     * Called from the page sheet. It delegates setting the image of the given [page] as the
     * cover to the presenter.
     */
    fun setAsCover(page: WatcherPage) {
        presenter.setAsCover(page)
    }

    /**
     * Called from the presenter when a page is set as cover or fails. It shows a different message
     * depending on the [result].
     */
    fun onSetAsCoverResult(result: WatcherPresenter.SetAsCoverResult) {
        toast(
            when (result) {
                Success -> R.string.cover_updated
                AddToLibraryFirst -> R.string.notification_first_add_to_library
                Error -> R.string.notification_cover_update_failed
            }
        )
    }

    /**
     * Forces the user preferred [orientation] on the activity.
     */
    private fun setOrientation(orientation: Int) {
        val newOrientation = OrientationType.fromPreference(orientation)
        if (newOrientation.flag != requestedOrientation) {
            requestedOrientation = newOrientation.flag
        }
    }

    /**
     * Class that handles the user preferences of the watcher.
     */
    private inner class WatcherConfig {

        /**
         * Initializes the watcher subscriptions.
         */
        init {
            preferences.rotation().asImmediateFlow { setOrientation(it) }
                .drop(1)
                .onEach {
                    delay(250)
                    setOrientation(it)
                }
                .launchIn(lifecycleScope)

            preferences.watcherTheme().asFlow()
                .drop(1) // We only care about updates
                .onEach { recreate() }
                .launchIn(lifecycleScope)

            preferences.showPageNumber().asFlow()
                .onEach { setPageNumberVisibility(it) }
                .launchIn(lifecycleScope)

            preferences.trueColor().asFlow()
                .onEach { setTrueColor(it) }
                .launchIn(lifecycleScope)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                preferences.cutoutShort().asFlow()
                    .onEach { setCutoutShort(it) }
                    .launchIn(lifecycleScope)
            }

            preferences.keepScreenOn().asFlow()
                .onEach { setKeepScreenOn(it) }
                .launchIn(lifecycleScope)

            preferences.customBrightness().asFlow()
                .onEach { setCustomBrightness(it) }
                .launchIn(lifecycleScope)

            preferences.colorFilter().asFlow()
                .onEach { setColorFilter(it) }
                .launchIn(lifecycleScope)

            preferences.colorFilterMode().asFlow()
                .onEach { setColorFilter(preferences.colorFilter().get()) }
                .launchIn(lifecycleScope)
        }

        /**
         * Sets the visibility of the bottom page indicator according to [visible].
         */
        fun setPageNumberVisibility(visible: Boolean) {
            binding.pageNumber.isVisible = visible
        }

        /**
         * Sets the 32-bit color mode according to [enabled].
         */
        private fun setTrueColor(enabled: Boolean) {
            if (enabled) {
                SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
            } else {
                SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.RGB_565)
            }
        }

        @TargetApi(Build.VERSION_CODES.P)
        private fun setCutoutShort(enabled: Boolean) {
            window.attributes.layoutInDisplayCutoutMode = when (enabled) {
                true -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                false -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            // Trigger relayout
            setMenuVisibility(menuVisible)
        }

        /**
         * Sets the keep screen on mode according to [enabled].
         */
        private fun setKeepScreenOn(enabled: Boolean) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        /**
         * Sets the custom brightness overlay according to [enabled].
         */
        private fun setCustomBrightness(enabled: Boolean) {
            if (enabled) {
                preferences.customBrightnessValue().asFlow()
                    .sample(100)
                    .onEach { setCustomBrightnessValue(it) }
                    .launchIn(lifecycleScope)
            } else {
                setCustomBrightnessValue(0)
            }
        }

        /**
         * Sets the color filter overlay according to [enabled].
         */
        private fun setColorFilter(enabled: Boolean) {
            if (enabled) {
                preferences.colorFilterValue().asFlow()
                    .sample(100)
                    .onEach { setColorFilterValue(it) }
                    .launchIn(lifecycleScope)
            } else {
                binding.colorOverlay.isVisible = false
            }
        }

        /**
         * Sets the brightness of the screen. Range is [-75, 100].
         * From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
         * From 1 to 100 it sets that value as brightness.
         * 0 sets system brightness and hides the overlay.
         */
        private fun setCustomBrightnessValue(value: Int) {
            // Calculate and set watcher brightness.
            val watcherBrightness = when {
                value > 0 -> {
                    value / 100f
                }
                value < 0 -> {
                    0.01f
                }
                else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }

            window.attributes = window.attributes.apply { screenBrightness = watcherBrightness }

            // Set black overlay visibility.
            if (value < 0) {
                binding.brightnessOverlay.isVisible = true
                val alpha = (abs(value) * 2.56).toInt()
                binding.brightnessOverlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
            } else {
                binding.brightnessOverlay.isVisible = false
            }
        }

        /**
         * Sets the color filter [value].
         */
        private fun setColorFilterValue(value: Int) {
            binding.colorOverlay.isVisible = true
            binding.colorOverlay.setFilterColor(value, preferences.colorFilterMode().get())
        }
    }
}
