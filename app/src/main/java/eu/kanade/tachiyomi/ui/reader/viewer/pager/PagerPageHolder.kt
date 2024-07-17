package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import okio.Buffer
import okio.BufferedSource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.ImageUtil
import tachiyomi.core.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import java.io.BufferedInputStream
import java.io.InputStream
import kotlin.math.max

/**
 * View of the ViewPager that contains a page of a chapter.
 */
@SuppressLint("ViewConstructor")
class PagerPageHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val page: ReaderPage,
    private var extraPage: ReaderPage? = null,
) : ReaderPageImageView(readerThemedContext), ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item
        get() = page to extraPage

    /**
     * Loading progress bar to indicate the current progress.
     */
    private val progressIndicator: ReaderProgressIndicator = ReaderProgressIndicator(readerThemedContext)

    /**
     * Error layout to show when the image fails to load.
     */
    private var errorLayout: ReaderErrorBinding? = null

    private val scope = MainScope()

    /**
     * Job for loading the page and processing changes to the page's status.
     */
    private var loadJob: Job? = null

    /**
     * Job for loading the page.
     */
    private var extraLoadJob: Job? = null

    init {
        addView(progressIndicator)
        loadJob = scope.launch { loadPageAndProcessStatus(1) }
        extraLoadJob = scope.launch { loadPageAndProcessStatus(2) }
    }

    /**
     * Called when this view is detached from the window. Unsubscribes any active subscription.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadJob?.cancel()
        loadJob = null
        extraLoadJob?.cancel()
        extraLoadJob = null
    }

    /**
     * Loads the page and processes changes to the page's status.
     *
     * Returns immediately if the page has no PageLoader.
     * Otherwise, this function does not return. It will continue to process status changes until
     * the Job is cancelled.
     */
    private suspend fun loadPageAndProcessStatus(pageIndex: Int) {
        // SY -->
        val page = if (pageIndex == 1) page else extraPage
        page ?: return
        // SY <--
        val loader = page.chapter.pageLoader ?: return
        supervisorScope {
            launchIO {
                loader.loadPage(page)
            }
            page.statusFlow.collectLatest { state ->
                when (state) {
                    Page.State.QUEUE -> setQueued()
                    Page.State.LOAD_PAGE -> setLoading()
                    Page.State.DOWNLOAD_IMAGE -> {
                        setDownloading()
                        page.progressFlow.collectLatest { value ->
                            progressIndicator.setProgress(value)
                        }
                    }
                    Page.State.READY -> setImage()
                    Page.State.ERROR -> setError()
                }
            }
        }
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is downloading.
     */
    private fun setDownloading() {
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is ready.
     */
    @Suppress("MagicNumber", "LongMethod")
    private suspend fun setImage() {
        if (extraPage == null) {
            progressIndicator.setProgress(0)
        } else {
            progressIndicator.setProgress(95)
        }

        val streamFn = page.stream ?: return
        val streamFn2 = extraPage?.stream

        try {
            val (source, isAnimated, background) = withIOContext {
                streamFn().buffered(16).use { stream ->
                    // SY -->
                    (
                        if (extraPage != null) {
                            streamFn2?.invoke()
                                ?.buffered(16)
                        } else {
                            null
                        }
                        ).use { stream2 ->
                        if (viewer.config.dualPageSplit) {
                            process(item.first, stream)
                        } else {
                            mergePages(stream, stream2)
                        }.use { itemStream ->
                            // SY <--
                            val source = streamFn().use { process(item, Buffer().readFrom(it)) }
                            val isAnimated = ImageUtil.isAnimatedAndSupported(source)
                            val background = if (!isAnimated && viewer.config.automaticBackground) {
                                ImageUtil.chooseBackground(context, source.peek().inputStream())
                            } else {
                                null
                            }
                            Triple(source, isAnimated, background)
                        }
                    }
            }
            withUIContext {
                setImage(
                    source,
                    isAnimated,
                    Config(
                        zoomDuration = viewer.config.doubleTapAnimDuration,
                        minimumScaleType = viewer.config.imageScaleType,
                        cropBorders = viewer.config.imageCropBorders,
                        zoomStartPosition = viewer.config.imageZoomType,
                        landscapeZoom = viewer.config.landscapeZoom,
                    ),
                )
                if (!isAnimated) {
                    pageBackground = background
                }
                removeErrorLayout()
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
            withUIContext {
                setError()
            }
        }
    }

    private fun process(page: ReaderPage, imageSource: BufferedSource): BufferedSource {
        if (viewer.config.dualPageRotateToFit) {
            return rotateDualPage(imageSource)
        }

        if (!viewer.config.dualPageSplit) {
            return imageSource
        }

        if (page is InsertPage) {
            return splitInHalf(imageSource)
        }
        val isDoublePage = ImageUtil.isWideImage(
            imageSource,
            // SY -->
            page.zip4jFile,
            page.zip4jEntry,
            // SY <--
        )
        if (!isDoublePage) {
            return imageSource
        }

        onPageSplit(page)

        return splitInHalf(imageSource)
    }

    private fun rotateDualPage(imageSource: BufferedSource): BufferedSource {
        val isDoublePage = ImageUtil.isWideImage(
            imageSource,
            // SY -->
            page.zip4jFile,
            page.zip4jEntry,
            // SY <--
        )
        return if (isDoublePage) {
            val rotation = if (viewer.config.dualPageRotateToFitInvert) -90f else 90f
            ImageUtil.rotateImage(imageSource, rotation)
        } else {
            imageSource
        }
    }

    @Suppress(
        "ReturnCount",
        "TooGenericExceptionCaught",
        "MagicNumber",
        "LongMethod",
        "CyclomaticComplexMethod",
        "ComplexCondition"
    )
    private fun mergePages(imageSource: BufferedSource, imageStream2: InputStream?): BufferedSource {
        // Handle adding a center margin to wide images if requested
        if (imageStream2 == null) {
            return if (imageSource is BufferedInputStream &&
                !ImageUtil.isAnimatedAndSupported(imageSource) &&
                ImageUtil.isWideImage(
                    imageSource,
                    // SY -->
                    page.zip4jFile,
                    page.zip4jEntry,
                    // SY <--
                ) &&
                viewer.config.centerMarginType and PagerConfig.CenterMarginType.WIDE_PAGE_CENTER_MARGIN > 0 &&
                !viewer.config.imageCropBorders
            ) {
                ImageUtil.addHorizontalCenterMargin(imageStream, height, context)
            } else {
                imageSource
            }
        }

        if (page.fullPage) return imageSource
        if (ImageUtil.isAnimatedAndSupported(imageSource)) {
            page.fullPage = true
            splitDoublePages()
            return imageSource
        } else if (ImageUtil.isAnimatedAndSupported(imageStream2)) {
            page.isolatedPage = true
            extraPage?.fullPage = true
            splitDoublePages()
            return imageSource
        }
        val imageBytes = imageSource.readBytes()
        val imageBitmap = try {
            ImageDecoder.newInstance(imageBytes.inputStream())?.decode()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Cannot combine pages" }
            null
        }
        if (imageBitmap == null) {
            imageStream2.close()
            imageSource.close()
            page.fullPage = true
            splitDoublePages()
            logcat(LogPriority.ERROR) { "Cannot combine pages" }
            return imageBytes.inputStream()
        }
        scope.launch { progressIndicator.setProgress(96) }
        val height = imageBitmap.height
        val width = imageBitmap.width

        if (height < width) {
            imageStream2.close()
            imageSource.close()
            page.fullPage = true
            splitDoublePages()
            return imageBytes.inputStream()
        }

        val imageBytes2 = imageStream2.readBytes()
        val imageBitmap2 = try {
            ImageDecoder.newInstance(imageBytes2.inputStream())?.decode()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Cannot combine pages" }
            null
        }
        if (imageBitmap2 == null) {
            imageStream2.close()
            imageSource.close()
            extraPage?.fullPage = true
            page.isolatedPage = true
            splitDoublePages()
            logcat(LogPriority.ERROR) { "Cannot combine pages" }
            return imageBytes.inputStream()
        }
        scope.launch { progressIndicator.setProgress(97) }
        val height2 = imageBitmap2.height
        val width2 = imageBitmap2.width

        if (height2 < width2) {
            imageStream2.close()
            imageSource.close()
            extraPage?.fullPage = true
            page.isolatedPage = true
            splitDoublePages()
            return imageBytes.inputStream()
        }
        val isLTR = (viewer !is R2LPagerViewer) xor viewer.config.invertDoublePages

        imageSource.close()
        imageStream2.close()

        val centerMargin = if (viewer.config.centerMarginType and PagerConfig.CenterMarginType
                .DOUBLE_PAGE_CENTER_MARGIN > 0 && !viewer.config.imageCropBorders
        ) {
            96 / (this.height.coerceAtLeast(1) / max(height, height2).coerceAtLeast(1)).coerceAtLeast(1)
        } else {
            0
        }

        return ImageUtil.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, centerMargin, viewer.config.pageCanvasColor) {
            scope.launch {
                if (it == 100) {
                    progressIndicator.hide()
                } else {
                    progressIndicator.setProgress(it)
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun splitDoublePages() {
        scope.launch {
            delay(100)
            viewer.splitDoublePages(page)
            if (extraPage?.fullPage == true || page.fullPage) {
                extraPage = null
            }
        }
    }

    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    private fun splitInHalf(imageSource: BufferedSource): BufferedSource {
        var side = when {
            viewer is L2RPagerViewer && page is InsertPage -> ImageUtil.Side.RIGHT
            viewer !is L2RPagerViewer && page is InsertPage -> ImageUtil.Side.LEFT
            viewer is L2RPagerViewer && page !is InsertPage -> ImageUtil.Side.LEFT
            viewer !is L2RPagerViewer && page !is InsertPage -> ImageUtil.Side.RIGHT
            else -> error("We should choose a side!")
        }

        if (viewer.config.dualPageInvert) {
            side = when (side) {
                ImageUtil.Side.RIGHT -> ImageUtil.Side.LEFT
                ImageUtil.Side.LEFT -> ImageUtil.Side.RIGHT
            }
        }

        val sideMargin = if ((
                viewer.config.centerMarginType and PagerConfig.CenterMarginType
                    .DOUBLE_PAGE_CENTER_MARGIN
                ) > 0 &&
            viewer.config.doublePages && !viewer.config.imageCropBorders
        ) {
            48
        } else {
            0
        }

        return ImageUtil.splitInHalf(imageSource, side, sideMargin)
    }

    private fun onPageSplit(page: ReaderPage) {
        val newPage = InsertPage(page)
        viewer.onPageSplit(page, newPage)
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() {
        progressIndicator.hide()
        showErrorLayout()
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        progressIndicator.hide()
    }

    /**
     * Called when an image fails to decode.
     */
    override fun onImageLoadError() {
        super.onImageLoadError()
        setError()
    }

    /**
     * Called when an image is zoomed in/out.
     */
    override fun onScaleChanged(newScale: Float) {
        super.onScaleChanged(newScale)
        viewer.activity.hideMenu()
    }

    private fun showErrorLayout(): ReaderErrorBinding {
        if (errorLayout == null) {
            errorLayout = ReaderErrorBinding.inflate(LayoutInflater.from(context), this, true)
            errorLayout?.actionRetry?.viewer = viewer
            errorLayout?.actionRetry?.setOnClickListener {
                page.chapter.pageLoader?.retryPage(page)
            }
        }

        val imageUrl = page.imageUrl
        errorLayout?.actionOpenInWebView?.isVisible = imageUrl != null
        if (imageUrl != null) {
            if (imageUrl.startsWith("http", true)) {
                errorLayout?.actionOpenInWebView?.viewer = viewer
                errorLayout?.actionOpenInWebView?.setOnClickListener {
                    val intent = WebViewActivity.newIntent(context, imageUrl)
                    context.startActivity(intent)
                }
            }
        }

        errorLayout?.root?.isVisible = true
        return errorLayout!!
    }

    /**
     * Removes the decode error layout from the holder, if found.
     */
    private fun removeErrorLayout() {
        errorLayout?.root?.isVisible = false
        errorLayout = null
    }
}
