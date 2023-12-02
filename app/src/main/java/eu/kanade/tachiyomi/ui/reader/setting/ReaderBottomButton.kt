package eu.kanade.tachiyomi.ui.reader.setting

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class ReaderBottomButton(val value: String, @StringRes val stringRes: Int) {
    ViewChapters("vc", R.string.action_view_chapters),
    WebView("wb", R.string.action_open_in_web_view),
    Share("sh", R.string.action_share),
    ReadingMode("rm", R.string.viewer),
    Rotation("rot", R.string.rotation_type),
    Crop("crop", R.string.pref_crop_borders),
    PageLayout("pl", R.string.page_layout),
    ;

    fun isIn(buttons: Collection<String>) = value in buttons

    companion object {
        val BUTTONS_DEFAULTS = setOf(
            ViewChapters,
            ReadingMode,
            Rotation,
            Crop,
            PageLayout,
        ).map { it.value }.toSet()
    }
}
