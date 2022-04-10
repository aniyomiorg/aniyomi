package eu.kanade.tachiyomi.data.coil

import coil.key.Keyer
import coil.request.Options
import eu.kanade.tachiyomi.data.database.models.Anime

class AnimeCoverKeyer : Keyer<Anime> {
    override fun key(data: Anime, options: Options): String? {
        return data.thumbnail_url?.takeIf { it.isNotBlank() }
    }
}
