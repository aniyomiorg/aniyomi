package tachiyomi.source.local.image.manga

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import java.io.InputStream

expect class LocalMangaCoverManager {

    fun find(mangaUrl: String): UniFile?

    fun update(manga: SManga, inputStream: InputStream): UniFile?
}
