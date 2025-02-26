package tachiyomi.source.local.image.anime

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import java.io.InputStream

expect class LocalEpisodeThumbnailManager {

    fun find(animeUrl: String, fileName: String): UniFile?

    fun update(anime: SAnime, episode: SEpisode, inputStream: InputStream): UniFile?
}
