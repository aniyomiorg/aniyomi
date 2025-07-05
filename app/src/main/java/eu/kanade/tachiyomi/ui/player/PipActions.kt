/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR

fun createPipActions(
    context: Context,
    isPaused: Boolean,
    replaceWithPrevious: Boolean,
    playlistCount: Int,
    playlistPosition: Int,
): ArrayList<RemoteAction> = arrayListOf(
    if (replaceWithPrevious) {
        createPipAction(
            context,
            R.drawable.ic_skip_previous_24dp,
            AYMR.strings.action_previous_episode,
            PIP_PREVIOUS,
            PIP_PREVIOUS,
            playlistPosition != 0,
        )
    } else {
        createPipAction(
            context,
            R.drawable.ic_forward_10_24dp,
            AYMR.strings.pref_skip_10,
            PIP_SKIP,
            PIP_SKIP,
        )
    },
    if (isPaused) {
        createPipAction(
            context,
            R.drawable.ic_play_arrow_24dp,
            AYMR.strings.action_play,
            PIP_PLAY,
            PIP_PLAY,
        )
    } else {
        createPipAction(
            context,
            R.drawable.ic_pause_24dp,
            MR.strings.action_pause,
            PIP_PAUSE,
            PIP_PAUSE,
        )
    },
    createPipAction(
        context,
        R.drawable.ic_skip_next_24dp,
        AYMR.strings.action_next_episode,
        PIP_NEXT,
        PIP_NEXT,
        playlistPosition != playlistCount - 1,
    ),
)

fun createPipAction(
    context: Context,
    @DrawableRes icon: Int,
    titleRes: StringResource,
    requestCode: Int,
    controlType: Int,
    isEnabled: Boolean = true,
): RemoteAction {
    val action = RemoteAction(
        Icon.createWithResource(context, icon),
        context.stringResource(titleRes),
        context.stringResource(titleRes),
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(PIP_INTENTS_FILTER).putExtra(PIP_INTENT_ACTION, controlType).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )
    action.isEnabled = isEnabled
    return action
}

const val PIP_INTENTS_FILTER = "pip_control"
const val PIP_INTENT_ACTION = "media_control"
const val PIP_PAUSE = 1
const val PIP_PLAY = 2
const val PIP_PREVIOUS = 3
const val PIP_NEXT = 4
const val PIP_SKIP = 5
