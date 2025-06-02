package eu.kanade.presentation.more.settings.screen.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.tachiyomi.ui.player.SingleActionGesture
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object PlayerSettingsGesturesScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = AYMR.strings.pref_player_gestures

    @Composable
    override fun getPreferences(): List<Preference> {
        val gesturePreferences = remember { Injekt.get<GesturePreferences>() }

        return listOf(
            getSlidersGroup(gesturePreferences = gesturePreferences),
            getSeekingGroup(gesturePreferences = gesturePreferences),
            getDoubleTapGroup(gesturePreferences = gesturePreferences),
            getMediaControlsGroup(gesturePreferences = gesturePreferences),
        )
    }

    @Composable
    private fun getSlidersGroup(gesturePreferences: GesturePreferences): Preference.PreferenceGroup {
        val enableVolumeBrightnessGestures = gesturePreferences.gestureVolumeBrightness()
        val swapVol = gesturePreferences.swapVolumeBrightness()

        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_category_player_sliders),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = enableVolumeBrightnessGestures,
                    title = stringResource(AYMR.strings.enable_volume_brightness_gestures),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = swapVol,
                    title = stringResource(AYMR.strings.pref_controls_swap_vol_brightness),
                ),
            ),
        )
    }

    @Composable
    private fun getSeekingGroup(gesturePreferences: GesturePreferences): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val enableHorizontalSeekGesture = gesturePreferences.gestureHorizontalSeek()
        val showSeekbar = gesturePreferences.showSeekBar()
        val defaultSkipIntroLength by gesturePreferences.defaultIntroLength().stateIn(scope).collectAsState()
        val skipLengthPreference = gesturePreferences.skipLengthPreference()
        val playerSmoothSeek = gesturePreferences.playerSmoothSeek()

        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            SkipIntroLengthDialog(
                initialSkipIntroLength = defaultSkipIntroLength,
                onDismissRequest = { showDialog = false },
                onValueChanged = { skipIntroLength ->
                    gesturePreferences.defaultIntroLength().set(skipIntroLength)
                    showDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_category_player_seeking),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = enableHorizontalSeekGesture,
                    title = stringResource(AYMR.strings.enable_horizontal_seek_gesture),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = showSeekbar,
                    title = stringResource(AYMR.strings.pref_show_seekbar),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(AYMR.strings.pref_default_intro_length),
                    subtitle = "${defaultSkipIntroLength}s",
                    onClick = { showDialog = true },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = skipLengthPreference,
                    entries = persistentMapOf(
                        30 to stringResource(AYMR.strings.pref_skip_30),
                        20 to stringResource(AYMR.strings.pref_skip_20),
                        10 to stringResource(AYMR.strings.pref_skip_10),
                        5 to stringResource(AYMR.strings.pref_skip_5),
                        3 to stringResource(AYMR.strings.pref_skip_3),
                        0 to stringResource(AYMR.strings.pref_skip_disable),
                    ),
                    title = stringResource(AYMR.strings.pref_skip_length),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = playerSmoothSeek,
                    title = stringResource(AYMR.strings.pref_player_smooth_seek),
                    subtitle = stringResource(AYMR.strings.pref_player_smooth_seek_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getDoubleTapGroup(gesturePreferences: GesturePreferences): Preference.PreferenceGroup {
        val leftDoubleTap = gesturePreferences.leftDoubleTapGesture()
        val centerDoubleTap = gesturePreferences.centerDoubleTapGesture()
        val rightDoubleTap = gesturePreferences.rightDoubleTapGesture()

        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_category_double_tap),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = leftDoubleTap,
                    entries = listOf(
                        SingleActionGesture.None,
                        SingleActionGesture.Seek,
                        SingleActionGesture.PlayPause,
                        SingleActionGesture.Switch,
                        SingleActionGesture.Custom,
                    ).associateWith { stringResource(it.stringRes) }.toPersistentMap(),
                    title = stringResource(AYMR.strings.pref_left_double_tap),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = centerDoubleTap,
                    entries = listOf(
                        SingleActionGesture.None,
                        SingleActionGesture.PlayPause,
                        SingleActionGesture.Custom,
                    ).associateWith { stringResource(it.stringRes) }.toPersistentMap(),
                    title = stringResource(AYMR.strings.pref_center_double_tap),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = rightDoubleTap,
                    entries = listOf(
                        SingleActionGesture.None,
                        SingleActionGesture.Seek,
                        SingleActionGesture.PlayPause,
                        SingleActionGesture.Switch,
                        SingleActionGesture.Custom,
                    ).associateWith { stringResource(it.stringRes) }.toPersistentMap(),
                    title = stringResource(AYMR.strings.pref_right_double_tap),
                ),
                Preference.PreferenceItem.InfoPreference(
                    title = stringResource(AYMR.strings.pref_double_tap_info),
                ),
            ),
        )
    }

    @Composable
    private fun getMediaControlsGroup(gesturePreferences: GesturePreferences): Preference.PreferenceGroup {
        val mediaPrevious = gesturePreferences.mediaPreviousGesture()
        val mediaPlayPause = gesturePreferences.mediaPlayPauseGesture()
        val mediaNext = gesturePreferences.mediaNextGesture()

        return Preference.PreferenceGroup(
            title = stringResource(AYMR.strings.pref_category_media_controls),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    preference = mediaPrevious,
                    entries = listOf(
                        SingleActionGesture.None,
                        SingleActionGesture.Seek,
                        SingleActionGesture.PlayPause,
                        SingleActionGesture.Switch,
                        SingleActionGesture.Custom,
                    ).associateWith { stringResource(it.stringRes) }.toPersistentMap(),
                    title = stringResource(AYMR.strings.pref_media_previous),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = mediaPlayPause,
                    entries = listOf(
                        SingleActionGesture.None,
                        SingleActionGesture.PlayPause,
                        SingleActionGesture.Custom,
                    ).associateWith { stringResource(it.stringRes) }.toPersistentMap(),
                    title = stringResource(AYMR.strings.pref_media_playpause),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = mediaNext,
                    entries = listOf(
                        SingleActionGesture.None,
                        SingleActionGesture.Seek,
                        SingleActionGesture.PlayPause,
                        SingleActionGesture.Switch,
                        SingleActionGesture.Custom,
                    ).associateWith { stringResource(it.stringRes) }.toPersistentMap(),
                    title = stringResource(AYMR.strings.pref_media_next),
                ),
                Preference.PreferenceItem.InfoPreference(
                    title = stringResource(AYMR.strings.pref_media_info),
                ),
            ),
        )
    }

    @Composable
    fun SkipIntroLengthDialog(
        initialSkipIntroLength: Int,
        onDismissRequest: () -> Unit,
        onValueChanged: (skipIntroLength: Int) -> Unit,
    ) {
        val skipIntroLengthValue by rememberSaveable { mutableStateOf(initialSkipIntroLength) }
        var newLength = 0
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(AYMR.strings.pref_intro_length)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        WheelTextPicker(
                            modifier = Modifier.align(Alignment.Center),
                            items = remember { 0..255 }.map {
                                stringResource(
                                    MR.strings.seconds_short,
                                    it,
                                )
                            }.toImmutableList(),
                            onSelectionChanged = {
                                newLength = it
                            },
                            startIndex = skipIntroLengthValue,
                        )
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChanged(newLength) }) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
        )
    }
}
