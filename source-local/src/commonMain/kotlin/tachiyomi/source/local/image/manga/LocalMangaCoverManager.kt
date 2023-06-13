package tachiyomi.source.local.image.manga

import eu.kanade.tachiyomi.source.model.SManga
import java.io.File
import java.io.InputStream

expect class LocalMangaCoverManager {

    fun find(mangaUrl: String): File?

    fun update(manga: SManga, inputStream: InputStream): File?
}
