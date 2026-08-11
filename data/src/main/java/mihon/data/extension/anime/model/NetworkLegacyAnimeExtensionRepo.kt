package mihon.data.extension.anime.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mihon.domain.extension.anime.model.AnimeExtensionStore

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkLegacyAnimeExtensionRepo(
    @SerialName("index_v2")
    val indexV2: String?,
    val meta: Meta,
) : BaseNetworkAnimeExtensionStore {
    @Serializable
    data class Meta(
        val name: String,
        val shortName: String?,
        val website: String,
        val signingKeyFingerprint: String,
    )

    override fun toExtensionStore(indexUrl: String): AnimeExtensionStore {
        return AnimeExtensionStore(
            indexUrl = indexUrl,
            name = meta.name,
            badgeLabel = meta.shortName ?: meta.name,
            signingKey = meta.signingKeyFingerprint,
            contact = AnimeExtensionStore.Contact(
                website = meta.website,
                discord = null,
            ),
            isLegacy = true,
            extensionListUrl = null,
        )
    }
}
