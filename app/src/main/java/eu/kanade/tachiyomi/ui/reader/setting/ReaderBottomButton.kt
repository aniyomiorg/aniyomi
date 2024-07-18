package eu.kanade.tachiyomi.ui.reader.setting

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class ReaderBottomButton(val value: String, val stringRes: StringResource) {
    WebView("wb", MR.strings.action_open_in_web_view),
    Share("sh", MR.strings.action_share),
    ReadingMode("rm", MR.strings.viewer),
    Rotation("rot", MR.strings.rotation_type),
    Crop("cro", MR.strings.pref_crop_borders),
    PageLayout("pl", MR.strings.page_layout),
    ;

    fun isIn(buttons: Collection<String>) = value in buttons

    companion object {
        val BUTTONS_DEFAULTS = setOf(
            ReadingMode,
            Rotation,
            Crop,
            PageLayout,
        ).map { it.value }.toSet()
    }
}
