package tachiyomi.source.local.entries.utils

import android.content.Context
import com.hippo.unifile.UniFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

/**
 * Manages a local, persistent cache of directory contents to reduce redundant file system operations.
 *
 * This cache works around a known Storage Access Framework (SAF)
 * performance issue: https://issuetracker.google.com/issues/130261278
 */
class LocalCacheManager(
    private val context: Context,
    private val baseDirectory: UniFile?,
    private val label: String,
) {
    /**
     * In-memory cache of the directory snapshot.
     *
     * Holds a lightweight representation of the directory contents to avoid repeated expensive calls
     * to [UniFile.name] and [UniFile.lastModified] during frequent access.
     */
    private var snapshot: Snapshot by AtomicReference(Snapshot())

    /**
     * Retrieves the list of files in the base directory, using the cached snapshot if available.
     *
     * @return A list of [UniFileLite] representing the current contents of the base directory.
     */
    suspend fun getFilesInBaseDirectory(): List<UniFileLite> = withIOContext {
        snapshot.value.takeIf(List<UniFileLite>::isNotEmpty)
            ?: baseDirectory?.listFiles().orEmpty().toList()
                .map {
                    // Access to these properties is slow due to SAF
                    async {
                        UniFileLite(it.name.orEmpty(), it.lastModified(), it.isDirectory)
                    }
                }
                .awaitAll()
                .also {
                    snapshot = Snapshot(
                        value = it,
                        lastModified = baseDirectory?.lastModified() ?: 0L,
                    )
                    updateSnapshot()
                }
    }

    // The filename is derived from the [label] to support multiple independent caches.
    private val snapshotFile: File
        get() = File(context.cacheDir, "${label}_local_cache_v1")

    fun loadAndVerifyCache() {
        loadCache()
        verifyCacheTimeStamp()
    }

    private fun loadCache() {
        if (!snapshot.isEmpty()) return
        loadSnapshotFile()
    }

    private fun loadSnapshotFile() {
        if (!snapshotFile.exists()) return
        try {
            snapshot = snapshotFile.inputStream().use {
                ProtoBuf.decodeFromByteArray<Snapshot>(it.readBytes())
            }
        } catch (_: Exception) {
            snapshotFile.delete()
        }
    }

    private fun updateSnapshot() {
        val bytes = ProtoBuf.encodeToByteArray(snapshot)
        try {
            snapshotFile.writeBytes(bytes)
        } catch (e: Throwable) {
            logcat(
                priority = LogPriority.ERROR,
                throwable = e,
                message = { "Failed to write snapshot file" },
            )
        }
    }

    private fun verifyCacheTimeStamp() {
        val lastModified = baseDirectory?.lastModified()
            ?: return snapshotFile.run { delete() }

        if (snapshot.isUpToDate(lastModified)) return

        snapshot = Snapshot()
        snapshotFile.delete()
        updateSnapshot()
    }
}

/**
 * A snapshot of the state of the cached directory
 */
@Serializable
class Snapshot(
    var value: List<UniFileLite> = emptyList(),
    private var lastModified: Long = 0,
) {
    fun isEmpty() = value.isEmpty()
    fun isUpToDate(lastModified: Long) = this.lastModified >= lastModified
}

/**
 * A lightweight, serializable representation of [UniFile] used for caching.
 */
@Serializable
class UniFileLite(
    val name: String,
    private val lastModified: Long,
    val isDirectory: Boolean,
) {
    fun lastModified() = lastModified
}

operator fun <T> AtomicReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T =
    this.get()

operator fun <T> AtomicReference<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.set(value)
}
