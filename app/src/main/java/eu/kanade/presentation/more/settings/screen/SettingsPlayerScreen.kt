package eu.kanade.presentation.more.settings.screen

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import com.chargemap.compose.numberpicker.NumberPicker
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import eu.kanade.tachiyomi.util.preference.asState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsPlayerScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    @StringRes
    override fun getTitleRes() = R.string.pref_category_player

    @Composable
    override fun getPreferences(): List<Preference> {
        val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }

        return listOf(
            Preference.PreferenceItem.ListPreference(
                pref = playerPreferences.progressPreference(),
                title = stringResource(R.string.pref_progress_mark_as_seen),
                entries = mapOf(
                    1.00F to stringResource(R.string.pref_progress_100),
                    0.95F to stringResource(R.string.pref_progress_95),
                    0.90F to stringResource(R.string.pref_progress_90),
                    0.85F to stringResource(R.string.pref_progress_85),
                    0.80F to stringResource(R.string.pref_progress_80),
                    0.75F to stringResource(R.string.pref_progress_75),
                    0.70F to stringResource(R.string.pref_progress_70),
                ),
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = playerPreferences.preserveWatchingPosition(),
                title = stringResource(R.string.pref_preserve_watching_position),
            ),
            getOrientationGroup(playerPreferences = playerPreferences),
            getAniskipGroup(playerPreferences = playerPreferences),
            getInternalPlayerGroup(playerPreferences = playerPreferences, basePreferences = basePreferences),
            getExternalPlayerGroup(playerPreferences = playerPreferences, basePreferences = basePreferences),
        )
    }

    @Composable
    private fun getOrientationGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val defaultPlayerOrientationType = playerPreferences.defaultPlayerOrientationType()
        val adjustOrientationVideoDimensions = playerPreferences.adjustOrientationVideoDimensions()
        val defaultPlayerOrientationPortrait = playerPreferences.defaultPlayerOrientationPortrait()
        val defaultPlayerOrientationLandscape = playerPreferences.defaultPlayerOrientationLandscape()

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_player_orientation),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationType,
                    title = stringResource(R.string.pref_default_player_orientation),
                    entries = mapOf(
                        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR to stringResource(R.string.rotation_free),
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to stringResource(R.string.rotation_portrait),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT to stringResource(R.string.rotation_reverse_portrait),
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to stringResource(R.string.rotation_landscape),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE to stringResource(R.string.rotation_reverse_landscape),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT to stringResource(R.string.rotation_sensor_portrait),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE to stringResource(R.string.rotation_sensor_landscape),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = adjustOrientationVideoDimensions,
                    title = stringResource(R.string.pref_adjust_orientation_video_dimensions),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationPortrait,
                    title = stringResource(R.string.pref_default_portrait_orientation),
                    entries = mapOf(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to stringResource(R.string.rotation_portrait),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT to stringResource(R.string.rotation_reverse_portrait),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT to stringResource(R.string.rotation_sensor_portrait),
                    ),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = defaultPlayerOrientationLandscape,
                    title = stringResource(R.string.pref_default_landscape_orientation),
                    entries = mapOf(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to stringResource(R.string.rotation_landscape),
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE to stringResource(R.string.rotation_reverse_landscape),
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE to stringResource(R.string.rotation_sensor_landscape),
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun getAniskipGroup(playerPreferences: PlayerPreferences): Preference.PreferenceGroup {
        val enableAniSkip = playerPreferences.aniSkipEnabled()
        val enableAutoAniSkip = playerPreferences.autoSkipAniSkip()
        val enableNetflixAniSkip = playerPreferences.enableNetflixStyleAniSkip()
        val waitingTimeAniSkip = playerPreferences.waitingTimeAniSkip()

        val isAniSkipEnabled by enableAniSkip.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_player_aniskip),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableAniSkip,
                    title = stringResource(R.string.pref_enable_aniskip),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableAutoAniSkip,
                    title = stringResource(R.string.pref_enable_auto_skip_ani_skip),
                    enabled = isAniSkipEnabled,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = enableNetflixAniSkip,
                    title = stringResource(R.string.pref_enable_netflix_style_aniskip),
                    enabled = isAniSkipEnabled,
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = waitingTimeAniSkip,
                    title = stringResource(R.string.pref_waiting_time_aniskip),
                    entries = mapOf(
                        5 to stringResource(R.string.pref_waiting_time_aniskip_5),
                        6 to stringResource(R.string.pref_waiting_time_aniskip_6),
                        7 to stringResource(R.string.pref_waiting_time_aniskip_7),
                        8 to stringResource(R.string.pref_waiting_time_aniskip_8),
                        9 to stringResource(R.string.pref_waiting_time_aniskip_9),
                        10 to stringResource(R.string.pref_waiting_time_aniskip_10),
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun getInternalPlayerGroup(playerPreferences: PlayerPreferences, basePreferences: BasePreferences): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val defaultSkipIntroLength by playerPreferences.skipLengthPreference().stateIn(scope).collectAsState()
        val skipLengthPreference = playerPreferences.skipLengthPreference()
        val playerSmoothSeek = playerPreferences.playerSmoothSeek()
        val playerFullscreen = playerPreferences.playerFullscreen()
        val playerHideControls = playerPreferences.hideControls()
        val pipEpisodeToasts = playerPreferences.pipEpisodeToasts()
        val pipOnExit = playerPreferences.pipOnExit()
        val mpvConf = playerPreferences.mpvConf()

        val deviceHasPip = basePreferences.context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            SkipIntroLengthDialog(
                initialSkipIntroLength = defaultSkipIntroLength,
                onDismissRequest = { showDialog = false },
                onValueChanged = { skipIntroLength ->
                    playerPreferences.skipLengthPreference().set(skipIntroLength)
                    showDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_internal_player),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_default_intro_length),
                    subtitle = "${defaultSkipIntroLength}s",
                    onClick = { showDialog = true },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = skipLengthPreference,
                    title = stringResource(R.string.pref_skip_length),
                    entries = mapOf(
                        30 to stringResource(R.string.pref_skip_30),
                        20 to stringResource(R.string.pref_skip_20),
                        10 to stringResource(R.string.pref_skip_10),
                        5 to stringResource(R.string.pref_skip_5),
                        0 to stringResource(R.string.pref_skip_disable),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = playerSmoothSeek,
                    title = stringResource(R.string.pref_player_smooth_seek),
                    subtitle = stringResource(R.string.pref_player_smooth_seek_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = playerFullscreen,
                    title = stringResource(R.string.pref_player_fullscreen),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = playerHideControls,
                    title = stringResource(R.string.pref_player_hide_controls),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipEpisodeToasts,
                    title = stringResource(R.string.pref_pip_episode_toasts),
                    enabled = deviceHasPip,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = pipOnExit,
                    title = stringResource(R.string.pref_pip_on_exit),
                    enabled = deviceHasPip,
                ),
                Preference.PreferenceItem.MultiLineEditTextPreference(
                    pref = mpvConf,
                    title = stringResource(R.string.pref_mpv_conf),
                    subtitle = mpvConf.asState(scope).value
                        .lines().take(2)
                        .joinToString(
                            separator = "\n",
                            postfix = if (mpvConf.asState(scope).value.lines().size > 2) "\n..." else "",
                        ),

                ),
            ),
        )
    }

    @Composable
    private fun getExternalPlayerGroup(playerPreferences: PlayerPreferences, basePreferences: BasePreferences): Preference.PreferenceGroup {
        val alwaysUseExternalPlayer = playerPreferences.alwaysUseExternalPlayer()
        val externalPlayerPreference = playerPreferences.externalPlayerPreference()

        val pm = basePreferences.context.packageManager
        val installedPackages = pm.getInstalledPackages(0)
        val supportedPlayers = installedPackages.filter {
            when (it.packageName) {
                "is.xyz.mpv" -> true
                "com.mxtech.videoplayer" -> true
                "com.mxtech.videoplayer.ad" -> true
                "com.mxtech.videoplayer.pro" -> true
                "org.videolan.vlc" -> true
                "com.husudosu.mpvremote" -> true
                else -> false
            }
        }
        val packageNames = supportedPlayers.map { it.packageName }
        val packageNamesReadable = supportedPlayers
            .map { pm.getApplicationLabel(it.applicationInfo).toString() }

        val packageNamesMap: Map<String, String> =
            packageNames.zip(packageNamesReadable)
                .toMap()

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_external_player),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = alwaysUseExternalPlayer,
                    title = stringResource(R.string.pref_always_use_external_player),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = externalPlayerPreference,
                    title = stringResource(R.string.pref_external_player_preference),
                    entries = mapOf("" to "None") + packageNamesMap,
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
        var skipIntroLengthValue by rememberSaveable { mutableStateOf(initialSkipIntroLength) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(R.string.pref_intro_length)) },
            text = {
                Row {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NumberPicker(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clipToBounds(),
                            value = skipIntroLengthValue,
                            onValueChange = { skipIntroLengthValue = it },
                            range = 1..255,
                            label = { it.toString() },
                            dividersColor = MaterialTheme.colorScheme.primary,
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChanged(skipIntroLengthValue) }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }
}
