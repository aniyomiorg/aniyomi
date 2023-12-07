package eu.kanade.tachiyomi.ui.player.viewer

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PictureInPictureHandler(
    private val activity: PlayerActivity,
    private val pipEnabled: Boolean,
) {

    internal val supportedAndEnabled: Boolean
        get() = Injekt.get<BasePreferences>().deviceHasPip() && pipEnabled

    internal fun start() {
        if (PipState.mode == PipState.ON) return
        if (supportedAndEnabled) {
            PipState.mode = PipState.STARTED

            activity.playerControls.hideControls(hide = true)
            activity.player.paused
                ?.let { update(!it) }
                ?.let { activity.enterPictureInPictureMode(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        iconResId: Int,
        titleRes: StringResource,
        requestCode: Int,
        controlType: Int,
        isEnabled: Boolean = true,
    ): RemoteAction {
        val action = RemoteAction(
            Icon.createWithResource(activity, iconResId),
            activity.stringResource(titleRes),
            activity.stringResource(titleRes),
            PendingIntent.getBroadcast(
                activity,
                requestCode,
                Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        action.isEnabled = isEnabled
        return action
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(
        playing: Boolean,
    ): PictureInPictureParams {
        var aspect: Int? = null
        if (activity.player.videoAspect != null) {
            aspect = if (activity.player.videoAspect!!.times(10000) >= 23900) {
                23899
            } else if (activity.player.videoAspect!!.times(10000) <= 4184) {
                4185
            } else {
                activity.player.videoAspect!!.times(10000).toInt()
            }
        }
        val plCount = activity.viewModel.currentPlaylist.size
        val plPos = activity.viewModel.getCurrentEpisodeIndex()
        val mPictureInPictureParams = PictureInPictureParams.Builder()
            // Set action items for the picture-in-picture mode. These are the only custom controls
            // available during the picture-in-picture mode.
            .setActions(
                arrayListOf(
                    if (activity.playerPreferences.pipReplaceWithPrevious().get()) {
                        createRemoteAction(
                            R.drawable.ic_skip_previous_24dp,
                            MR.strings.action_previous_episode,
                            PIP_PREVIOUS,
                            PIP_PREVIOUS,
                            plPos != 0
                        )
                    } else {
                        createRemoteAction(
                            R.drawable.ic_forward_10_24dp,
                            MR.strings.pref_skip_10,
                            PIP_SKIP,
                            PIP_SKIP,
                        )
                    },
                    if (playing) {
                        createRemoteAction(
                            R.drawable.ic_pause_24dp,
                            MR.strings.action_pause,
                            PIP_PAUSE,
                            PIP_PAUSE,
                        )
                    } else {
                        createRemoteAction(
                            R.drawable.ic_play_arrow_24dp,
                            MR.strings.action_play,
                            PIP_PLAY,
                            PIP_PLAY,
                        )
                    },
                    createRemoteAction(
                        R.drawable.ic_skip_next_24dp,
                        MR.strings.action_next_episode,
                        PIP_NEXT,
                        PIP_NEXT,
                        plPos != plCount - 1
                    ),
                ),
            )
            .setAspectRatio(aspect?.let { Rational(it, 10000) })
            .build()
        activity.setPictureInPictureParams(mPictureInPictureParams)
        return mPictureInPictureParams
    }
}

internal const val PIP_PLAY = 1
internal const val PIP_PAUSE = 2
internal const val PIP_PREVIOUS = 3
internal const val PIP_NEXT = 4
internal const val PIP_SKIP = 5

internal const val ACTION_MEDIA_CONTROL = "media_control"
internal const val EXTRA_CONTROL_TYPE = "control_type"
