package tachiyomi.source.local.image.anime

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.model.SAnime
import java.io.InputStream

expect class LocalAnimeBackgroundManager {

    fun find(animeUrl: String): UniFile?

    fun update(anime: SAnime, inputStream: InputStream): UniFile?
}
