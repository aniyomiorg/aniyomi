package tachiyomi.source.local.image.anime

import eu.kanade.tachiyomi.animesource.model.SAnime
import java.io.File
import java.io.InputStream

expect class LocalAnimeCoverManager {

    fun find(animeUrl: String): File?

    fun update(anime: SAnime, inputStream: InputStream): File?
}
