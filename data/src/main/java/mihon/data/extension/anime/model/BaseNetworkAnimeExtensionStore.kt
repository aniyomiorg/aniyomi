package mihon.data.extension.anime.model

import mihon.domain.extension.anime.model.AnimeExtensionStore

interface BaseNetworkAnimeExtensionStore {
    fun toExtensionStore(indexUrl: String): AnimeExtensionStore
}
