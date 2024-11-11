package eu.kanade.tachiyomi.ui.player.settings

import eu.kanade.tachiyomi.ui.player.viewer.HwDecState
import eu.kanade.tachiyomi.ui.player.viewer.VideoDebanding
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class DecoderPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun tryHWDecoding() = preferenceStore.getBoolean("pref_try_hwdec", true)
    fun gpuNext() = preferenceStore.getBoolean("pref_gpu_next", false)
    fun videoDebanding() = preferenceStore.getEnum("pref_video_debanding", VideoDebanding.NONE)
    fun useYUV420P() = preferenceStore.getBoolean("use_yuv420p", true)

    // Non-preferences

    fun hardwareDecoding() = preferenceStore.getEnum("pref_hardware_decoding", HwDecState.defaultHwDec)
}
