package eu.kanade.presentation.more.settings.screen

import android.content.pm.ActivityInfo
import android.os.Build
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.player.JUST_PLAYER
import eu.kanade.tachiyomi.ui.player.MPV_PLAYER
import eu.kanade.tachiyomi.ui.player.MPV_REMOTE
import eu.kanade.tachiyomi.ui.player.MX_PLAYER
import eu.kanade.tachiyomi.ui.player.MX_PLAYER_FREE
import eu.kanade.tachiyomi.ui.player.MX_PLAYER_PRO
import eu.kanade.tachiyomi.ui.player.NEXT_PLAYER
import eu.kanade.tachiyomi.ui.player.VLC_PLAYER
import eu.kanade.tachiyomi.ui.player.WEB_VIDEO_CASTER
import eu.kanade.tachiyomi.ui.player.X_PLAYER
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.viewer.AudioChannels
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsPlayerScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_player

    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val deviceSupportsPip = basePreferences.deviceHasPip()

        return listOfNotNull(
            Preference.PreferenceItem.ListPreference(
                pref = playerPreferences.progressPreference(),
                title = stringResource(MR.strings.pref_progress_mark_as_seen),
                entries = persistentMapOf(
                    1.00F to stringResource(MR.strings.pref_progress_100),
                    0.95F to stringResource(MR.strings.pref_progress_95),
                    0.90F to stringResource(MR.strings.pref_progress_90),
                    0.85F to stringResource(MR.strings.pref_progress_85),
                    0.80F to stringResource(MR.strings.pref_progress_80),
                    0.75F to stringResource(MR.strings.pref_progress_75),
                    0.70F to stringResource(MR.strings.pref_progress_70),
                ),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = playerPreferences.preserveWatchingPosition(),
                title = stringResource(MR.strings.pref_preserve_watching_position),
            ),
            getInternalPlayerGroup(playerPreferences = playerPreferences),
            getVolumeAndBrightnessGroup(playerPreferences = playerPreferences),
            getOrientationGroup(playerPreferences = playerPreferences),
            getSeekingGroup(playerPreferences = playerPreferences),
            if (deviceSupportsPip) getPipGroup(playerPreferences = playerPreferences) else null,
            getExternalPlayerGroup(
                playerPreferences = playerPreferences,
                basePreferences = basePreferences,
            ),
        )
    }

    @Composable
    private fun getInternalPlayerGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val playerFullscreen = playerPreferences.playerFullscreen()
        val playerHideControls = playerPreferences.hideControls()
        val playerAudioChannels = playerPreferences.audioChannels()
        val navigator = LocalNavigator.currentOrThrow

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_internal_player),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = playerFullscreen,
                    title = stringResource(MR.strings.pref_player_fullscreen),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = playerHideControls,
                    title = stringResource(MR.strings.pref_player_hide_controls),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = playerAudioChannels,
                    title = stringResource(MR.strings.pref_player_audio_channels),
                    entries = persistentMapOf(
                        AudioChannels.AutoSafe to stringResource(AudioChannels.AutoSafe.textRes),
                        AudioChannels.Auto to stringResource(AudioChannels.Auto.textRes),
                        AudioChannels.Mono to stringResource(AudioChannels.Mono.textRes),
                        AudioChannels.Stereo to stringResource(AudioChannels.Stereo.textRes),
                        AudioChannels.ReverseStereo to stringResource(AudioChannels.ReverseStereo.textRes),
                    ),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_category_player_advanced),
                    subtitle = stringResource(MR.strings.pref_category_player_advanced_subtitle),
                    onClick = { navigator.push(AdvancedPlayerSettingsScreen) },
                ),
            ),
        )
    }

    @Composable
    private fun getVolumeAndBrightnessGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val enableVolumeBrightnessGestures = playerPreferences.gestureVolumeBrightness()
        val rememberPlayerBrightness = playerPreferences.rememberPlayerBrightness()
        val rememberPlayerVolume = playerPreferences.rememberPlayerVolume()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_volume_brightness),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableVolumeBrightnessGestures,
                    title = stringResource(MR.strings.enable_volume_brightness_gestures),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = rememberPlayerBrightness,
                    title = stringResource(MR.strings.pref_remember_brightness),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = rememberPlayerVolume,
                    title = stringResource(MR.strings.pref_remember_volume),
                ),
            ),
        )
    }

    @Composable
    private fun getOrientationGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val defaultPlayerOrientationType = playerPreferences.defaultPlayerOrientationType()
        val adjustOrientationVideoDimensions = playerPreferences.adjustOrientationVideoDimensions()
        val defaultPlayerOrientationPortrait = playerPreferences.defaultPlayerOrientationPortrait()
        val defaultPlayerOrientationLandscape = playerPreferences.defaultPlayerOrientationLandscape()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_player_orientation),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationType,
                    title = stringResource(MR.strings.pref_default_player_orientation),
                    entries = persistentMapOf(
                        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR to stringResource(
                            MR.strings.rotation_free,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to stringResource(
                            MR.strings.rotation_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT to stringResource(
                            MR.strings.rotation_reverse_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to stringResource(
                            MR.strings.rotation_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE to stringResource(
                            MR.strings.rotation_reverse_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT to stringResource(
                            MR.strings.rotation_sensor_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE to stringResource(
                            MR.strings.rotation_sensor_landscape,
                        ),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = adjustOrientationVideoDimensions,
                    title = stringResource(MR.strings.pref_adjust_orientation_video_dimensions),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationPortrait,
                    title = stringResource(MR.strings.pref_default_portrait_orientation),
                    entries = persistentMapOf(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to stringResource(
                            MR.strings.rotation_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT to stringResource(
                            MR.strings.rotation_reverse_portrait,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT to stringResource(
                            MR.strings.rotation_sensor_portrait,
                        ),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationLandscape,
                    title = stringResource(MR.strings.pref_default_landscape_orientation),
                    entries = persistentMapOf(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to stringResource(
                            MR.strings.rotation_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE to stringResource(
                            MR.strings.rotation_reverse_landscape,
                        ),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE to stringResource(
                            MR.strings.rotation_sensor_landscape,
                        ),
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun getSeekingGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val enableHorizontalSeekGesture = playerPreferences.gestureHorizontalSeek()
        val defaultSkipIntroLength by playerPreferences.defaultIntroLength().stateIn(scope).collectAsState()
        val skipLengthPreference = playerPreferences.skipLengthPreference()
        val playerSmoothSeek = playerPreferences.playerSmoothSeek()
        val mediaChapterSeek = playerPreferences.mediaChapterSeek()

        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            SkipIntroLengthDialog(
                initialSkipIntroLength = defaultSkipIntroLength,
                onDismissRequest = { showDialog = false },
                onValueChanged = { skipIntroLength ->
                    playerPreferences.defaultIntroLength().set(skipIntroLength)
                    showDialog = false
                },
            )
        }

        // Aniskip
        val enableAniSkip = playerPreferences.aniSkipEnabled()
        val enableAutoAniSkip = playerPreferences.autoSkipAniSkip()
        val enableNetflixAniSkip = playerPreferences.enableNetflixStyleAniSkip()
        val waitingTimeAniSkip = playerPreferences.waitingTimeAniSkip()

        val isAniSkipEnabled by enableAniSkip.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_player_seeking),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableHorizontalSeekGesture,
                    title = stringResource(MR.strings.enable_horizontal_seek_gesture),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_default_intro_length),
                    subtitle = "${defaultSkipIntroLength}s",
                    onClick = { showDialog = true },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = skipLengthPreference,
                    title = stringResource(MR.strings.pref_skip_length),
                    entries = persistentMapOf(
                        30 to stringResource(MR.strings.pref_skip_30),
                        20 to stringResource(MR.strings.pref_skip_20),
                        10 to stringResource(MR.strings.pref_skip_10),
                        5 to stringResource(MR.strings.pref_skip_5),
                        3 to stringResource(MR.strings.pref_skip_3),
                        0 to stringResource(MR.strings.pref_skip_disable),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = playerSmoothSeek,
                    title = stringResource(MR.strings.pref_player_smooth_seek),
                    subtitle = stringResource(MR.strings.pref_player_smooth_seek_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = mediaChapterSeek,
                    title = stringResource(MR.strings.pref_media_control_chapter_seeking),
                    subtitle = stringResource(MR.strings.pref_media_control_chapter_seeking_summary),
                ),
                Preference.PreferenceItem.InfoPreference(
                    title = stringResource(MR.strings.pref_category_player_aniskip_info),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableAniSkip,
                    title = stringResource(MR.strings.pref_enable_aniskip),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableAutoAniSkip,
                    title = stringResource(MR.strings.pref_enable_auto_skip_ani_skip),
                    enabled = isAniSkipEnabled,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableNetflixAniSkip,
                    title = stringResource(MR.strings.pref_enable_netflix_style_aniskip),
                    enabled = isAniSkipEnabled,
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = waitingTimeAniSkip,
                    title = stringResource(MR.strings.pref_waiting_time_aniskip),
                    entries = persistentMapOf(
                        5 to stringResource(MR.strings.pref_waiting_time_aniskip_5),
                        6 to stringResource(MR.strings.pref_waiting_time_aniskip_6),
                        7 to stringResource(MR.strings.pref_waiting_time_aniskip_7),
                        8 to stringResource(MR.strings.pref_waiting_time_aniskip_8),
                        9 to stringResource(MR.strings.pref_waiting_time_aniskip_9),
                        10 to stringResource(MR.strings.pref_waiting_time_aniskip_10),
                    ),
                    enabled = isAniSkipEnabled,
                ),
            ),
        )
    }

    @Composable
    private fun getPipGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val enablePip = playerPreferences.enablePip()
        val pipEpisodeToasts = playerPreferences.pipEpisodeToasts()
        val pipOnExit = playerPreferences.pipOnExit()
        val pipReplaceWithPrevious = playerPreferences.pipReplaceWithPrevious()

        val isPipEnabled by enablePip.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_pip),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enablePip,
                    title = stringResource(MR.strings.pref_enable_pip),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipEpisodeToasts,
                    title = stringResource(MR.strings.pref_pip_episode_toasts),
                    enabled = isPipEnabled,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipOnExit,
                    title = stringResource(MR.strings.pref_pip_on_exit),
                    enabled = isPipEnabled,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipReplaceWithPrevious,
                    title = stringResource(MR.strings.pref_pip_replace_with_previous),
                    enabled = isPipEnabled,
                ),
            ),
        )
    }

    @Composable
    private fun getExternalPlayerGroup(
        playerPreferences: PlayerPreferences,
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val alwaysUseExternalPlayer = playerPreferences.alwaysUseExternalPlayer()
        val externalPlayerPreference = playerPreferences.externalPlayerPreference()

        val pm = basePreferences.context.packageManager
        val installedPackages = pm.getInstalledPackages(0)
        val supportedPlayers = installedPackages.filter { it.packageName in externalPlayers }

        val packageNames = supportedPlayers.map { it.packageName }
        val packageNamesReadable = supportedPlayers
            .map { pm.getApplicationLabel(it.applicationInfo).toString() }

        val packageNamesMap: Map<String, String> =
            packageNames.zip(packageNamesReadable)
                .toMap()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_external_player),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = alwaysUseExternalPlayer,
                    title = stringResource(MR.strings.pref_always_use_external_player),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = externalPlayerPreference,
                    title = stringResource(MR.strings.pref_external_player_preference),
                    entries = (mapOf("" to "None") + packageNamesMap).toPersistentMap(),
                ),
            ),
        )
    }

    @Composable
    private fun SkipIntroLengthDialog(
        initialSkipIntroLength: Int,
        onDismissRequest: () -> Unit,
        onValueChanged: (skipIntroLength: Int) -> Unit,
    ) {
        val skipIntroLengthValue by rememberSaveable { mutableStateOf(initialSkipIntroLength) }
        var newLength = 0
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(MR.strings.pref_intro_length)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        WheelTextPicker(
                            modifier = Modifier.align(Alignment.Center),
                            items = remember { 1..255 }.map {
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

val externalPlayers = listOf(
    MPV_PLAYER,
    MX_PLAYER,
    MX_PLAYER_FREE,
    MX_PLAYER_PRO,
    VLC_PLAYER,
    MPV_REMOTE,
    JUST_PLAYER,
    NEXT_PLAYER,
    X_PLAYER,
    WEB_VIDEO_CASTER,
)
