package eu.kanade.tachiyomi.ui.player.viewer

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR

class PictureInPictureHandler {

    fun update(
        context: Context,
        title: String,
        subtitle: String,
        paused: Boolean,
        replaceWithPrevious: Boolean,
        pipOnExit: Boolean,
        videoAspect: Double,
        playlistCount: Int,
        playlistPosition: Int,
    ): PictureInPictureParams {
        val aspectRatio = videoAspect.let { aspect ->
            when {
                aspect >= 23900 -> 23899
                aspect <= 4184 -> 4185
                else -> aspect.toInt()
            }
        }

        val pictureInPictureParams = PictureInPictureParams.Builder()
            .setActions(pipActions(context, paused, replaceWithPrevious, playlistCount, playlistPosition))
            .setAspectRatio(Rational(aspectRatio, 10000))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pictureInPictureParams.setAutoEnterEnabled(pipOnExit).setSeamlessResizeEnabled(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pictureInPictureParams.setTitle(title).setSubtitle(subtitle)
        }

        return pictureInPictureParams.build()
    }

    private fun pipActions(
        context: Context,
        paused: Boolean,
        replaceWithPrevious: Boolean,
        playlistCount: Int,
        playlistPosition: Int,
    ): ArrayList<RemoteAction> {
        return arrayListOf(
            if (replaceWithPrevious) {
                createRemoteAction(
                    context,
                    R.drawable.ic_skip_previous_24dp,
                    MR.strings.action_previous_episode,
                    PIP_PREVIOUS,
                    PIP_PREVIOUS,
                    playlistPosition != 0,
                )
            } else {
                createRemoteAction(
                    context,
                    R.drawable.ic_forward_10_24dp,
                    MR.strings.pref_skip_10,
                    PIP_SKIP,
                    PIP_SKIP,
                )
            },
            if (paused) {
                createRemoteAction(
                    context,
                    R.drawable.ic_play_arrow_24dp,
                    MR.strings.action_play,
                    PIP_PLAY,
                    PIP_PLAY,
                )
            } else {
                createRemoteAction(
                    context,
                    R.drawable.ic_pause_24dp,
                    MR.strings.action_pause,
                    PIP_PAUSE,
                    PIP_PAUSE,
                )
            },
            createRemoteAction(
                context,
                R.drawable.ic_skip_next_24dp,
                MR.strings.action_next_episode,
                PIP_NEXT,
                PIP_NEXT,
                playlistPosition != playlistCount - 1,
            ),
        )
    }

    private fun createRemoteAction(
        context: Context,
        iconResId: Int,
        titleRes: StringResource,
        requestCode: Int,
        controlType: Int,
        isEnabled: Boolean = true,
    ): RemoteAction {
        val action = RemoteAction(
            Icon.createWithResource(context, iconResId),
            context.stringResource(titleRes),
            context.stringResource(titleRes),
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        action.isEnabled = isEnabled
        return action
    }
}

// TODO: https://developer.android.com/develop/ui/views/picture-in-picture#setautoenterenabled

internal const val PIP_PLAY = 1
internal const val PIP_PAUSE = 2
internal const val PIP_PREVIOUS = 3
internal const val PIP_NEXT = 4
internal const val PIP_SKIP = 5

internal const val ACTION_MEDIA_CONTROL = "media_control"
internal const val EXTRA_CONTROL_TYPE = "control_type"
