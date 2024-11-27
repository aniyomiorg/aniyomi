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

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import `is`.xyz.mpv.Utils
import logcat.LogPriority
import logcat.logcat

internal fun Uri.openContentFd(context: Context): String? {
    return context.contentResolver.openFileDescriptor(this, "r")?.detachFd()?.let {
        Utils.findRealPath(it)?.also { _ ->
            ParcelFileDescriptor.adoptFd(it).close()
        } ?: "fd://$it"
    }
}

internal fun Uri.resolveUri(context: Context): String? {
    val filepath = when (scheme) {
        "file" -> path
        "content" -> openContentFd(context)
        "data" -> "data://$schemeSpecificPart"
        in Utils.PROTOCOLS -> toString()
        else -> null
    }

    if (filepath == null) logcat(LogPriority.ERROR) { "unknown scheme: $scheme" }
    return filepath
}

internal fun Uri.getFileName(context: Context): String? {
    return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    }
}
