package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.transition.doOnEnd
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.transition.platform.MaterialContainerTransform
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.connections.service.ConnectionsPreferences
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.presentation.reader.OrientationSelectDialog
import eu.kanade.presentation.reader.PageIndicatorText
import eu.kanade.presentation.reader.ReaderContentOverlay
import eu.kanade.presentation.reader.ReaderPageActionsDialog
import eu.kanade.presentation.reader.ReadingModeSelectDialog
import eu.kanade.presentation.reader.appbars.ReaderAppBars
import eu.kanade.presentation.reader.settings.ReaderSettingsDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.Constants
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.ReaderData
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.databinding.ReaderActivityBinding
import eu.kanade.tachiyomi.source.manga.isNsfw
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import eu.kanade.tachiyomi.util.system.isNightMode
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setComposeContent
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.i18n.stringResource
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.lang.launchUI
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ReaderActivity : BaseActivity() {

    companion object {
        fun newIntent(
            context: Context,
            mangaId: Long?,
            chapterId: Long?,
            page: Int? = null,
        ): Intent {
            return Intent(context, ReaderActivity::class.java).apply {
                putExtra("manga", mangaId)
                putExtra("chapter", chapterId)
                // SY -->
                putExtra("page", page)
                // SY <--
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        // AM (CONNECTIONS) -->
        private val connectionsPreferences: ConnectionsPreferences = Injekt.get()
        // <-- AM (CONNECTIONS)
    }

    private val readerPreferences = Injekt.get<ReaderPreferences>()
    private val preferences = Injekt.get<BasePreferences>()

    lateinit var binding: ReaderActivityBinding

    val viewModel by viewModels<ReaderViewModel>()
    private var assistUrl: String? = null

    private val hasCutout by lazy { hasDisplayCutout() }

    /**
     * Configuration at reader level, like background color or forced orientation.
     */
    private var config: ReaderConfig? = null

    private var menuToggleToast: Toast? = null
    private var readingModeToast: Toast? = null
    private val displayRefreshHost = DisplayRefreshHost()

    private val windowInsetsController by lazy { WindowInsetsControllerCompat(window, binding.root) }

    private var loadingIndicator: ReaderProgressIndicator? = null

    var isScrollingThroughPages = false
        private set

    /**
     * Called when the activity is created. Initializes the presenter and configuration.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        registerSecureActivity(this)
        overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)

        super.onCreate(savedInstanceState)

        binding = ReaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (viewModel.needsInit()) {
            val manga = intent.extras?.getLong("manga", -1) ?: -1L
            val chapter = intent.extras?.getLong("chapter", -1) ?: -1L
            // SY -->
            val page = intent.extras?.getInt("page", -1).takeUnless { it == -1 }
            // SY <--
            if (manga == -1L || chapter == -1L) {
                finish()
                return
            }
            NotificationReceiver.dismissNotification(
                this,
                manga.hashCode(),
                Notifications.ID_NEW_CHAPTERS,
            )

            lifecycleScope.launchNonCancellable {
                val initResult = viewModel.init(manga, chapter, page)
                if (!initResult.getOrDefault(false)) {
                    val exception = initResult.exceptionOrNull() ?: IllegalStateException(
                        "Unknown err",
                    )
                    withUIContext {
                        setInitialChapterError(exception)
                    }
                }
            }
        }

        config = ReaderConfig()
        initializeMenu()

        // Finish when incognito mode is disabled
        preferences.incognitoMode().changes()
            .drop(1)
            .onEach { if (!it) finish() }
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.isLoadingAdjacentChapter }
            .distinctUntilChanged()
            .onEach(::setProgressDialog)
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.manga }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { updateViewer() }
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.viewerChapters }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setChapters)
            .launchIn(lifecycleScope)

        viewModel.eventFlow
            .onEach { event ->
                when (event) {
                    ReaderViewModel.Event.ReloadViewerChapters -> {
                        viewModel.state.value.viewerChapters?.let(::setChapters)
                    }
                    ReaderViewModel.Event.PageChanged -> {
                        displayRefreshHost.flash()
                    }
                    is ReaderViewModel.Event.SetOrientation -> {
                        setOrientation(event.orientation)
                    }
                    is ReaderViewModel.Event.SavedImage -> {
                        onSaveImageResult(event.result)
                    }
                    is ReaderViewModel.Event.ShareImage -> {
                        onShareImageResult(event.uri, event.page, event.secondPage)
                    }
                    is ReaderViewModel.Event.SetCoverResult -> {
                        onSetAsCoverResult(event.result)
                    }
                }
            }
            .launchIn(lifecycleScope)
        viewModel.viewModelScope.launchUI {
            // AM (DISCORD) -->
            updateDiscordRPC(exitingReader = false)
            // <-- AM (DISCORD)
        }
    }

    /**
     * Called when the activity is destroyed. Cleans up the viewer, configuration and any view.
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.state.value.viewer?.destroy()
        config = null
        menuToggleToast?.cancel()
        readingModeToast?.cancel()
        // AM (DISCORD) -->
        updateDiscordRPC(exitingReader = true)
        // <-- AM (DISCORD)
    }

    override fun onPause() {
        viewModel.flushReadTimer()
        super.onPause()
    }

    /**
     * Set menu visibility again on activity resume to apply immersive mode again if needed.
     * Helps with rotations.
     */
    override fun onResume() {
        super.onResume()
        viewModel.restartReadTimer()
        setMenuVisibility(viewModel.state.value.menuVisible)
    }

    /**
     * Called when the window focus changes. It sets the menu visibility to the last known state
     * to apply immersive mode again if needed.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(viewModel.state.value.menuVisible)
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        assistUrl?.let { outContent.webUri = it.toUri() }
    }

    /**
     * Called when the user clicks the back key or the button on the toolbar. The call is
     * delegated to the presenter.
     */
    override fun finish() {
        viewModel.onActivityFinish()
        super.finish()
        overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_N) {
            loadNextChapter()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_P) {
            loadPreviousChapter()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * Dispatches a key event. If the viewer doesn't handle it, call the default implementation.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewModel.state.value.viewer?.handleKeyEvent(event) ?: false
        return handled || super.dispatchKeyEvent(event)
    }

    /**
     * Dispatches a generic motion event. If the viewer doesn't handle it, call the default
     * implementation.
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = viewModel.state.value.viewer?.handleGenericMotionEvent(event) ?: false
        return handled || super.dispatchGenericMotionEvent(event)
    }

    /**
     * Initializes the reader menu. It sets up click listeners and the initial visibility.
     */
    private fun initializeMenu() {
        binding.pageNumber.setComposeContent {
            val state by viewModel.state.collectAsState()
            val showPageNumber by viewModel.readerPreferences.showPageNumber().collectAsState()

            if (!state.menuVisible && showPageNumber) {
                PageIndicatorText(
                    // SY -->
                    currentPage = state.currentPageText,
                    // SY <--
                    totalPages = state.totalPages,
                )
            }
        }

        binding.dialogRoot.setComposeContent {
            val state by viewModel.state.collectAsState()
            val settingsScreenModel = remember {
                ReaderSettingsScreenModel(
                    readerState = viewModel.state,
                    hasDisplayCutout = hasCutout,
                    onChangeReadingMode = viewModel::setMangaReadingMode,
                    onChangeOrientation = viewModel::setMangaOrientationType,
                )
            }

            val isHttpSource = viewModel.getSource() is HttpSource
            val isFullscreen by readerPreferences.fullscreen().collectAsState()
            val flashOnPageChange by readerPreferences.flashOnPageChange().collectAsState()

            val colorOverlayEnabled by readerPreferences.colorFilter().collectAsState()
            val colorOverlay by readerPreferences.colorFilterValue().collectAsState()
            val colorOverlayMode by readerPreferences.colorFilterMode().collectAsState()
            val colorOverlayBlendMode = remember(colorOverlayMode) {
                ReaderPreferences.ColorFilterMode.getOrNull(colorOverlayMode)?.second
            }

            val cropBorderPaged by readerPreferences.cropBorders().collectAsState()
            val cropBorderWebtoon by readerPreferences.cropBordersWebtoon().collectAsState()
            val isPagerType = ReadingMode.isPagerType(viewModel.getMangaReadingMode())
            val cropEnabled = if (isPagerType) cropBorderPaged else cropBorderWebtoon
            // SY -->
            val readerBottomButtons by readerPreferences.readerBottomButtons().changes().map { it.toImmutableSet() }
                .collectAsState(persistentSetOf())
            val dualPageSplitPaged by readerPreferences.dualPageSplitPaged().collectAsState()
            // SY <--

            ReaderContentOverlay(
                brightness = state.brightnessOverlayValue,
                color = colorOverlay.takeIf { colorOverlayEnabled },
                colorBlendMode = colorOverlayBlendMode,
            )

            ReaderAppBars(
                visible = state.menuVisible,
                fullscreen = isFullscreen,

                mangaTitle = state.manga?.title,
                chapterTitle = state.currentChapter?.chapter?.name,
                navigateUp = onBackPressedDispatcher::onBackPressed,
                onClickTopAppBar = ::openMangaScreen,
                bookmarked = state.bookmarked,
                onToggleBookmarked = viewModel::toggleChapterBookmark,
                onOpenInWebView = ::openChapterInWebView.takeIf { isHttpSource },
                onShare = ::shareChapter.takeIf { isHttpSource },

                viewer = state.viewer,
                onNextChapter = ::loadNextChapter,
                enabledNext = state.viewerChapters?.nextChapter != null,
                onPreviousChapter = ::loadPreviousChapter,
                enabledPrevious = state.viewerChapters?.prevChapter != null,
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                onSliderValueChange = {
                    isScrollingThroughPages = true
                    moveToPageIndex(it)
                },

                readingMode = ReadingMode.fromPreference(
                    viewModel.getMangaReadingMode(resolveDefault = false),
                ),
                onClickReadingMode = viewModel::openReadingModeSelectDialog,
                orientation = ReaderOrientation.fromPreference(
                    viewModel.getMangaOrientation(resolveDefault = false),
                ),
                onClickOrientation = viewModel::openOrientationModeSelectDialog,
                cropEnabled = cropEnabled,
                onClickCropBorder = {
                    val enabled = viewModel.toggleCropBorders()
                    menuToggleToast?.cancel()
                    menuToggleToast = toast(if (enabled) MR.strings.on else MR.strings.off)
                },
                onClickSettings = viewModel::openSettingsDialog,
                // SY -->
                currentPageText = state.currentPageText,
                enabledButtons = readerBottomButtons,
                dualPageSplitEnabled = dualPageSplitPaged,
                doublePages = state.doublePages,
                onClickPageLayout = {
                    if (readerPreferences.pageLayout().get() == PagerConfig.PageLayout.AUTOMATIC) {
                        (viewModel.state.value.viewer as? PagerViewer)?.config?.let { config ->
                            config.doublePages = !config.doublePages
                            reloadChapters(config.doublePages, true)
                        }
                    } else {
                        readerPreferences.pageLayout().set(1 - readerPreferences.pageLayout().get())
                    }
                },
                onClickShiftPage = ::shiftDoublePages,
                // SY <--
            )

            if (flashOnPageChange) {
                DisplayRefreshHost(
                    hostState = displayRefreshHost,
                )
            }

            val onDismissRequest = viewModel::closeDialog
            when (state.dialog) {
                is ReaderViewModel.Dialog.Loading -> {
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator()
                                Text(stringResource(MR.strings.loading))
                            }
                        },
                    )
                }
                is ReaderViewModel.Dialog.Settings -> {
                    ReaderSettingsDialog(
                        onDismissRequest = onDismissRequest,
                        onShowMenus = { setMenuVisibility(true) },
                        onHideMenus = { setMenuVisibility(false) },
                        screenModel = settingsScreenModel,
                    )
                }
                is ReaderViewModel.Dialog.ReadingModeSelect -> {
                    ReadingModeSelectDialog(
                        onDismissRequest = onDismissRequest,
                        screenModel = settingsScreenModel,
                        onChange = { stringRes ->
                            menuToggleToast?.cancel()
                            if (!readerPreferences.showReadingMode().get()) {
                                menuToggleToast = toast(stringRes)
                            }
                        },
                    )
                }
                is ReaderViewModel.Dialog.OrientationModeSelect -> {
                    OrientationSelectDialog(
                        onDismissRequest = onDismissRequest,
                        screenModel = settingsScreenModel,
                        onChange = { stringRes ->
                            menuToggleToast?.cancel()
                            menuToggleToast = toast(stringRes)
                        },
                    )
                }
                is ReaderViewModel.Dialog.PageActions -> {
                    ReaderPageActionsDialog(
                        onDismissRequest = onDismissRequest,
                        onSetAsCover = viewModel::setAsCover,
                        onShare = viewModel::shareImage,
                        onSave = viewModel::saveImage,
                        onShareCombined = viewModel::shareImages,
                        onSaveCombined = viewModel::saveImages,
                        hasExtraPage = (state.dialog as? ReaderViewModel.Dialog.PageActions)?.extraPage != null,
                    )
                }
                null -> {}
            }
        }

        val toolbarColor = ColorUtils.setAlphaComponent(
            SurfaceColors.SURFACE_2.getColor(this),
            if (isNightMode()) 230 else 242, // 90% dark 95% light
        )
        window.statusBarColor = toolbarColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = toolbarColor
        }

        // Set initial visibility
        setMenuVisibility(viewModel.state.value.menuVisible)
    }

    /**
     * Sets the visibility of the menu according to [visible].
     */
    private fun setMenuVisibility(visible: Boolean) {
        viewModel.showMenus(visible)
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        } else {
            if (readerPreferences.fullscreen().get()) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // SY -->

    fun reloadChapters(doublePages: Boolean, force: Boolean = false) {
        val viewer = viewModel.state.value.viewer as? PagerViewer ?: return
        viewer.updateShifting()
        if (!force && viewer.config.autoDoublePages) {
            setDoublePageMode(viewer)
        } else {
            viewer.config.doublePages = doublePages
            viewModel.setDoublePages(viewer.config.doublePages)
        }
        val currentChapter = viewModel.state.value.currentChapter
        if (doublePages) {
            // If we're moving from singe to double, we want the current page to be the first page
            val currentPage = viewModel.state.value.currentPage
            viewer.config.shiftDoublePage = (
                currentPage + (currentChapter?.pages?.take(currentPage)?.count { it.fullPage || it.isolatedPage } ?: 0)
                ) % 2 != 0
        }
        viewModel.state.value.viewerChapters?.let {
            viewer.setChaptersDoubleShift(it)
        }
    }

    private fun setDoublePageMode(viewer: PagerViewer) {
        val currentOrientation = resources.configuration.orientation
        viewer.config.doublePages = currentOrientation == Configuration.ORIENTATION_LANDSCAPE
        viewModel.setDoublePages(viewer.config.doublePages)
    }

    private fun shiftDoublePages() {
        val viewer = viewModel.state.value.viewer as? PagerViewer ?: return
        viewer.config.let { config ->
            config.shiftDoublePage = !config.shiftDoublePage
            viewModel.state.value.viewerChapters?.let {
                viewer.updateShifting()
                viewer.setChaptersDoubleShift(it)
                invalidateOptionsMenu()
            }
        }
    }

    // SY <--

    /**
     * Called from the presenter when a manga is ready. Used to instantiate the appropriate viewer.
     */
    private fun updateViewer() {
        val prevViewer = viewModel.state.value.viewer
        val newViewer = ReadingMode.toViewer(viewModel.getMangaReadingMode(), this)

        if (window.sharedElementEnterTransition is MaterialContainerTransform) {
            // Wait until transition is complete to avoid crash on API 26
            window.sharedElementEnterTransition.doOnEnd {
                setOrientation(viewModel.getMangaOrientation())
            }
        } else {
            setOrientation(viewModel.getMangaOrientation())
        }

        // Destroy previous viewer if there was one
        if (prevViewer != null) {
            prevViewer.destroy()
            binding.viewerContainer.removeAllViews()
        }
        viewModel.onViewerLoaded(newViewer)
        updateViewerInset(readerPreferences.fullscreen().get())
        binding.viewerContainer.addView(newViewer.getView())

        // SY -->
        if (newViewer is PagerViewer) {
            if (readerPreferences.pageLayout().get() == PagerConfig.PageLayout.AUTOMATIC) {
                setDoublePageMode(newViewer)
            }
            viewModel.state.value.lastShiftDoubleState?.let { newViewer.config.shiftDoublePage = it }
        }

        if (readerPreferences.showReadingMode().get()) {
            // SY <--
            showReadingModeToast(viewModel.getMangaReadingMode())
        }

        loadingIndicator = ReaderProgressIndicator(this)
        binding.readerContainer.addView(loadingIndicator)

        startPostponedEnterTransition()
    }

    private fun openMangaScreen() {
        viewModel.manga?.id?.let { id ->
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = Constants.SHORTCUT_MANGA
                    putExtra(Constants.MANGA_EXTRA, id)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            )
        }
    }

    private fun openChapterInWebView() {
        val manga = viewModel.manga ?: return
        val source = viewModel.getSource() ?: return
        assistUrl?.let {
            val intent = WebViewActivity.newIntent(this@ReaderActivity, it, source.id, manga.title)
            startActivity(intent)
        }
    }

    private fun shareChapter() {
        assistUrl?.let {
            val intent = it.toUri().toShareIntent(this, type = "text/plain")
            startActivity(Intent.createChooser(intent, stringResource(MR.strings.action_share)))
        }
    }

    private fun showReadingModeToast(mode: Int) {
        try {
            readingModeToast?.cancel()
            readingModeToast = toast(ReadingMode.fromPreference(mode).stringRes)
        } catch (e: ArrayIndexOutOfBoundsException) {
            logcat(LogPriority.ERROR) { "Unknown reading mode: $mode" }
        }
    }

    /**
     * Called from the presenter whenever a new [viewerChapters] have been set. It delegates the
     * method to the current viewer, but also set the subtitle on the toolbar, and
     * hides or disables the reader prev/next buttons if there's a prev or next chapter
     */
    @SuppressLint("RestrictedApi")
    private fun setChapters(viewerChapters: ViewerChapters) {
        binding.readerContainer.removeView(loadingIndicator)
        // SY -->
        val state = viewModel.state.value
        if (state.indexChapterToShift != null && state.indexPageToShift != null) {
            viewerChapters.currChapter.pages?.find {
                it.index == state.indexPageToShift && it.chapter.chapter.id == state.indexChapterToShift
            }?.let {
                (viewModel.state.value.viewer as? PagerViewer)?.updateShifting(it)
            }
            viewModel.setIndexChapterToShift(null)
            viewModel.setIndexPageToShift(null)
        } else if (state.lastShiftDoubleState != null) {
            val currentChapter = viewerChapters.currChapter
            (viewModel.state.value.viewer as? PagerViewer)?.config?.shiftDoublePage = (
                currentChapter.requestedPage +
                    (
                        currentChapter.pages?.take(currentChapter.requestedPage)
                            ?.count { it.fullPage || it.isolatedPage } ?: 0
                        )
                ) % 2 != 0
        }
        // SY <--

        viewModel.state.value.viewer?.setChapters(viewerChapters)

        lifecycleScope.launchIO {
            viewModel.getChapterUrl()?.let { url ->
                assistUrl = url
            }
        }
    }

    /**
     * Called from the presenter if the initial load couldn't load the pages of the chapter. In
     * this case the activity is closed and a toast is shown to the user.
     */
    private fun setInitialChapterError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
        finish()
        toast(error.message)
    }

    /**
     * Called from the presenter whenever it's loading the next or previous chapter. It shows or
     * dismisses a non-cancellable dialog to prevent user interaction according to the value of
     * [show]. This is only used when the next/previous buttons on the toolbar are clicked; the
     * other cases are handled with chapter transitions on the viewers and chapter preloading.
     */
    private fun setProgressDialog(show: Boolean) {
        if (show) {
            viewModel.showLoadingDialog()
        } else {
            viewModel.closeDialog()
        }
    }

    /**
     * Moves the viewer to the given page [index]. It does nothing if the viewer is null or the
     * page is not found.
     */
    private fun moveToPageIndex(index: Int) {
        val viewer = viewModel.state.value.viewer ?: return
        val currentChapter = viewModel.state.value.currentChapter ?: return
        val page = currentChapter.pages?.getOrNull(index) ?: return
        viewer.moveToPage(page)
    }

    /**
     * Tells the presenter to load the next chapter and mark it as active. The progress dialog
     * should be automatically shown.
     */
    private fun loadNextChapter() {
        lifecycleScope.launch {
            viewModel.loadNextChapter()
            moveToPageIndex(0)
        }
    }

    /**
     * Tells the presenter to load the previous chapter and mark it as active. The progress dialog
     * should be automatically shown.
     */
    private fun loadPreviousChapter() {
        lifecycleScope.launch {
            viewModel.loadPreviousChapter()
            moveToPageIndex(0)
        }
    }

    /**
     * Called from the viewer whenever a [page] is marked as active. It updates the values of the
     * bottom menu and delegates the change to the presenter.
     */
    @SuppressLint("SetTextI18n")
    fun onPageSelected(page: ReaderPage, hasExtraPage: Boolean = false) {
        // SY -->
        val currentPageText = if (hasExtraPage) {
            val invertDoublePage = (viewModel.state.value.viewer as? PagerViewer)?.config?.invertDoublePages ?: false
            if ((resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) xor invertDoublePage) {
                "${page.number}-${page.number + 1}"
            } else {
                "${page.number + 1}-${page.number}"
            }
        } else {
            "${page.number}"
        }
        viewModel.onPageSelected(page, currentPageText, hasExtraPage)
        // SY <--
    }

    /**
     * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
     * actions to perform is shown.
     */
    fun onPageLongTap(page: ReaderPage, extraPage: ReaderPage? = null) {
        // SY -->
        viewModel.openPageDialog(page, extraPage)
        // SY <--
    }

    /**
     * Called from the viewer when the given [chapter] should be preloaded. It should be called when
     * the viewer is reaching the beginning or end of a chapter or the transition page is active.
     */
    fun requestPreloadChapter(chapter: ReaderChapter) {
        lifecycleScope.launchIO { viewModel.preload(chapter) }
    }

    /**
     * Called from the viewer to toggle the visibility of the menu. It's implemented on the
     * viewer because each one implements its own touch and key events.
     */
    fun toggleMenu() {
        setMenuVisibility(!viewModel.state.value.menuVisible)
    }

    /**
     * Called from the viewer to show the menu.
     */
    fun showMenu() {
        if (!viewModel.state.value.menuVisible) {
            setMenuVisibility(true)
        }
    }

    /**
     * Called from the viewer to hide the menu.
     */
    fun hideMenu() {
        if (viewModel.state.value.menuVisible) {
            setMenuVisibility(false)
        }
    }

    /**
     * Called from the presenter when a page is ready to be shared. It shows Android's default
     * sharing tool.
     */
    fun onShareImageResult(uri: Uri, page: ReaderPage, secondPage: ReaderPage? = null) {
        val manga = viewModel.manga ?: return
        val chapter = page.chapter.chapter

        // SY -->
        val text = if (secondPage != null) {
            stringResource(
                MR.strings.share_pages_info,
                manga.title,
                chapter.name,
                if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                    "${page.number}-${page.number + 1}"
                } else {
                    "${page.number + 1}-${page.number}"
                },
            )
        } else {
            stringResource(MR.strings.share_page_info, manga.title, chapter.name, page.number)
        }
        // SY <--

        val intent = uri.toShareIntent(
            context = applicationContext,
            message = text,
        )
        startActivity(Intent.createChooser(intent, stringResource(MR.strings.action_share)))
    }

    /**
     * Called from the presenter when a page is saved or fails. It shows a message or logs the
     * event depending on the [result].
     */
    private fun onSaveImageResult(result: ReaderViewModel.SaveImageResult) {
        when (result) {
            is ReaderViewModel.SaveImageResult.Success -> {
                toast(MR.strings.picture_saved)
            }
            is ReaderViewModel.SaveImageResult.Error -> {
                logcat(LogPriority.ERROR, result.error)
            }
        }
    }

    /**
     * Called from the presenter when a page is set as cover or fails. It shows a different message
     * depending on the [result].
     */
    private fun onSetAsCoverResult(result: ReaderViewModel.SetAsCoverResult) {
        toast(
            when (result) {
                Success -> MR.strings.cover_updated
                AddToLibraryFirst -> MR.strings.notification_first_add_to_library
                Error -> MR.strings.notification_cover_update_failed
            },
        )
    }

    /**
     * Forces the user preferred [orientation] on the activity.
     */
    private fun setOrientation(orientation: Int) {
        val newOrientation = ReaderOrientation.fromPreference(orientation)
        if (newOrientation.flag != requestedOrientation) {
            requestedOrientation = newOrientation.flag
        }
    }

    /**
     * Updates viewer inset depending on fullscreen reader preferences.
     */
    private fun updateViewerInset(fullscreen: Boolean) {
        viewModel.state.value.viewer?.getView()?.applyInsetter {
            if (!fullscreen) {
                type(navigationBars = true, statusBars = true) {
                    padding()
                }
            }
        }
    }

    /**
     * Class that handles the user preferences of the reader.
     */
    private inner class ReaderConfig {

        private fun getCombinedPaint(grayscale: Boolean, invertedColors: Boolean): Paint {
            return Paint().apply {
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix().apply {
                        if (grayscale) {
                            setSaturation(0f)
                        }
                        if (invertedColors) {
                            postConcat(
                                ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f,
                                    ),
                                ),
                            )
                        }
                    },
                )
            }
        }

        private val grayBackgroundColor = Color.rgb(0x20, 0x21, 0x25)

        /**
         * Initializes the reader subscriptions.
         */
        init {
            readerPreferences.readerTheme().changes()
                .onEach { theme ->
                    binding.readerContainer.setBackgroundColor(
                        when (theme) {
                            0 -> Color.WHITE
                            2 -> grayBackgroundColor
                            3 -> automaticBackgroundColor()
                            else -> Color.BLACK
                        },
                    )
                }
                .launchIn(lifecycleScope)

            readerPreferences.trueColor().changes()
                .onEach(::setTrueColor)
                .launchIn(lifecycleScope)

            readerPreferences.cutoutShort().changes()
                .onEach(::setCutoutShort)
                .launchIn(lifecycleScope)

            readerPreferences.keepScreenOn().changes()
                .onEach(::setKeepScreenOn)
                .launchIn(lifecycleScope)

            readerPreferences.customBrightness().changes()
                .onEach(::setCustomBrightness)
                .launchIn(lifecycleScope)

            merge(readerPreferences.grayscale().changes(), readerPreferences.invertedColors().changes())
                .onEach { setLayerPaint(readerPreferences.grayscale().get(), readerPreferences.invertedColors().get()) }
                .launchIn(lifecycleScope)

            readerPreferences.fullscreen().changes()
                .onEach {
                    WindowCompat.setDecorFitsSystemWindows(window, !it)
                    updateViewerInset(it)
                }
                .launchIn(lifecycleScope)

            // SY -->
            readerPreferences.pageLayout().changes()
                .drop(1)
                .onEach {
                    viewModel.setDoublePages(
                        (viewModel.state.value.viewer as? PagerViewer)
                            ?.config
                            ?.doublePages
                            ?: false,
                    )
                }
                .launchIn(lifecycleScope)

            readerPreferences.dualPageSplitPaged().changes()
                .drop(1)
                .onEach {
                    if (viewModel.state.value.viewer !is PagerViewer) return@onEach
                    reloadChapters(
                        !it && when (readerPreferences.pageLayout().get()) {
                            PagerConfig.PageLayout.DOUBLE_PAGES -> true
                            PagerConfig.PageLayout.AUTOMATIC ->
                                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                            else -> false
                        },
                        true,
                    )
                }
                .launchIn(lifecycleScope)
            // SY <--
        }

        /**
         * Picks background color for [ReaderActivity] based on light/dark theme preference
         */
        private fun automaticBackgroundColor(): Int {
            return if (baseContext.isNightMode()) {
                grayBackgroundColor
            } else {
                Color.WHITE
            }
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

        private fun setCutoutShort(enabled: Boolean) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

            window.attributes.layoutInDisplayCutoutMode = when (enabled) {
                true -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                false -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            // Trigger relayout
            setMenuVisibility(viewModel.state.value.menuVisible)
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
                readerPreferences.customBrightnessValue().changes()
                    .sample(100)
                    .onEach(::setCustomBrightnessValue)
                    .launchIn(lifecycleScope)
            } else {
                setCustomBrightnessValue(0)
            }
        }

        /**
         * Sets the brightness of the screen. Range is [-75, 100].
         * From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
         * From 1 to 100 it sets that value as brightness.
         * 0 sets system brightness and hides the overlay.
         */
        private fun setCustomBrightnessValue(value: Int) {
            // Calculate and set reader brightness.
            val readerBrightness = when {
                value > 0 -> {
                    value / 100f
                }
                value < 0 -> {
                    0.01f
                }
                else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = window.attributes.apply { screenBrightness = readerBrightness }

            viewModel.setBrightnessOverlayValue(value)
        }
        private fun setLayerPaint(grayscale: Boolean, invertedColors: Boolean) {
            val paint = if (grayscale || invertedColors) getCombinedPaint(grayscale, invertedColors) else null
            binding.viewerContainer.setLayerType(LAYER_TYPE_HARDWARE, paint)
        }
    }

    // AM (DISCORD) -->
    private fun updateDiscordRPC(exitingReader: Boolean) {
        if (connectionsPreferences.enableDiscordRPC().get()) {
            viewModel.viewModelScope.launchIO {
                if (!exitingReader) {
                    DiscordRPCService.setReaderActivity(
                        context = this@ReaderActivity,
                        ReaderData(
                            incognitoMode = viewModel.currentSource.isNsfw() || viewModel.incognitoMode,
                            mangaId = viewModel.manga?.id,
                            // AM (CU)>
                            mangaTitle = viewModel.manga?.ogTitle,
                            thumbnailUrl = viewModel.manga?.thumbnailUrl,
                            chapterProgress = Pair(viewModel.state.value.currentPage, viewModel.state.value.totalPages),
                            chapterNumber =
                            if (connectionsPreferences.useChapterTitles().get()) {
                                viewModel.state.value.currentChapter?.chapter?.name
                            } else {
                                viewModel.state.value.currentChapter?.chapter?.chapter_number.toString()
                            },
                        ),
                    )
                } else {
                    val lastUsedScreen = DiscordRPCService.lastUsedScreen
                    DiscordRPCService.setMangaScreen(this@ReaderActivity, lastUsedScreen)
                }
            }
        }
    }
    // <-- AM (DISCORD)
}
