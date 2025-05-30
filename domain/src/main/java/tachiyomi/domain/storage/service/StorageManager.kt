package tachiyomi.domain.storage.service

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn

class StorageManager(
    private val context: Context,
    storagePreferences: StoragePreferences,
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private var baseDir: UniFile? = getBaseDir(storagePreferences.baseStorageDirectory().get())

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .shareIn(scope, SharingStarted.Lazily, 1)

    init {
        storagePreferences.baseStorageDirectory().changes()
            .drop(1)
            .distinctUntilChanged()
            .onEach { uri ->
                baseDir = getBaseDir(uri)
                baseDir?.let { parent ->
                    parent.createDirectory(AUTOMATIC_BACKUPS_PATH)
                    parent.createDirectory(LOCAL_SOURCE_PATH)
                    parent.createDirectory(LOCAL_ANIMESOURCE_PATH)
                    parent.createDirectory(DOWNLOADS_PATH).also {
                        DiskUtil.createNoMediaFile(it, context)
                    }
                    parent.createDirectory(MPV_CONFIG_PATH)?.let { mpvDir ->
                        mpvDir.createDirectory(FONTS_PATH)
                        mpvDir.createDirectory(SCRIPTS_PATH)
                        mpvDir.createDirectory(SCRIPT_OPTS_PATH)
                        mpvDir.createDirectory(SHADERS_PATH)
                    }
                }
                _changes.send(Unit)
            }
            .launchIn(scope)
    }

    private fun getBaseDir(uri: String): UniFile? {
        return UniFile.fromUri(context, uri.toUri())
            .takeIf { it?.exists() == true }
    }

    fun getAutomaticBackupsDirectory(): UniFile? {
        return baseDir?.createDirectory(AUTOMATIC_BACKUPS_PATH)
    }

    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(DOWNLOADS_PATH)
    }

    fun getLocalMangaSourceDirectory(): UniFile? {
        return baseDir?.createDirectory(LOCAL_SOURCE_PATH)
    }

    fun getLocalAnimeSourceDirectory(): UniFile? {
        return baseDir?.createDirectory(LOCAL_ANIMESOURCE_PATH)
    }

    fun getFontsDirectory(): UniFile? {
        return getMPVConfigDirectory()?.createDirectory(FONTS_PATH)
    }

    fun getScriptsDirectory(): UniFile? {
        return getMPVConfigDirectory()?.createDirectory(SCRIPTS_PATH)
    }

    fun getScriptOptsDirectory(): UniFile? {
        return getMPVConfigDirectory()?.createDirectory(SCRIPT_OPTS_PATH)
    }

    fun getShadersDirectory(): UniFile? {
        return getMPVConfigDirectory()?.createDirectory(SHADERS_PATH)
    }

    fun getMPVConfigDirectory(): UniFile? {
        return baseDir?.createDirectory(MPV_CONFIG_PATH)
    }
}

private const val AUTOMATIC_BACKUPS_PATH = "autobackup"
private const val DOWNLOADS_PATH = "downloads"
private const val LOCAL_SOURCE_PATH = "local"
private const val LOCAL_ANIMESOURCE_PATH = "localanime"
private const val MPV_CONFIG_PATH = "mpv-config"
private const val FONTS_PATH = "fonts"
const val SCRIPTS_PATH = "scripts"
const val SCRIPT_OPTS_PATH = "script-opts"
private const val SHADERS_PATH = "shaders"
