package eu.kanade.tachiyomi.ui.player.viewer

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.PlayerActivity
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
        titleResId: Int,
        requestCode: Int,
        controlType: Int,
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(activity, iconResId),
            activity.getString(titleResId),
            activity.getString(titleResId),
            PendingIntent.getBroadcast(
                activity,
                requestCode,
                Intent(ACTION_MEDIA_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
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
        val mPictureInPictureParams = PictureInPictureParams.Builder()
            // Set action items for the picture-in-picture mode. These are the only custom controls
            // available during the picture-in-picture mode.
            .setActions(
                arrayListOf(
                    createRemoteAction(
                        R.drawable.ic_skip_previous_24dp,
                        R.string.action_previous_episode,
                        CONTROL_TYPE_PREVIOUS,
                        REQUEST_PREVIOUS,
                    ),
                    if (playing) {
                        createRemoteAction(
                            R.drawable.ic_pause_24dp,
                            R.string.action_pause,
                            CONTROL_TYPE_PAUSE,
                            REQUEST_PAUSE,
                        )
                    } else {
                        createRemoteAction(
                            R.drawable.ic_play_arrow_24dp,
                            R.string.action_play,
                            CONTROL_TYPE_PLAY,
                            REQUEST_PLAY,
                        )
                    },
                    createRemoteAction(
                        R.drawable.ic_skip_next_24dp,
                        R.string.action_next_episode,
                        CONTROL_TYPE_NEXT,
                        REQUEST_NEXT,
                    ),
                ),
            )
            .setAspectRatio(aspect?.let { Rational(it, 10000) })
            .build()
        activity.setPictureInPictureParams(mPictureInPictureParams)
        return mPictureInPictureParams
    }
}

private const val REQUEST_PLAY = 1
private const val REQUEST_PAUSE = 2
private const val REQUEST_PREVIOUS = 3
private const val REQUEST_NEXT = 4

internal const val CONTROL_TYPE_PLAY = 1
internal const val CONTROL_TYPE_PAUSE = 2
internal const val CONTROL_TYPE_PREVIOUS = 3
internal const val CONTROL_TYPE_NEXT = 4

internal const val ACTION_MEDIA_CONTROL = "media_control"
internal const val EXTRA_CONTROL_TYPE = "control_type"
