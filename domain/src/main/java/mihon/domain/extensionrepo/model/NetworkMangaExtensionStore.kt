package mihon.domain.extensionrepo.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * basically a copy of Mihon's NetworkExtensionStore.kt
 * https://github.com/mihonapp/mihon/blob/main/data/src/main/java/mihon/data/extension/model/NetworkExtensionStore.kt
 * protobuf definition file
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkMangaExtensionStore(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val badgeLabel: String,
    @ProtoNumber(3) val signingKey: String,
    @ProtoNumber(4) val contact: Contact,
    @ProtoNumber(101) val extensionList: ExtensionList?,
    @ProtoNumber(102) val extensionListUrl: String?,
) {
    @Serializable
    data class Contact(
        @ProtoNumber(1) val website: String,
        @ProtoNumber(2) val discord: String?,
    )

    @Serializable
    data class ExtensionList(@ProtoNumber(1) val extensions: List<Extension>)

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val packageName: String,
        @ProtoNumber(3) val resources: Resources,
        @ProtoNumber(4) val extensionLib: String,
        @ProtoNumber(5) val versionCode: Long,
        @ProtoNumber(6) val versionName: String,
        @ProtoNumber(7) val contentWarning: ContentWarning,
        @ProtoNumber(8) val sources: List<Source>,
    )

    @Serializable
    data class Resources(
        @ProtoNumber(1) val apkUrl: String,
        @ProtoNumber(2) val iconUrl: String,
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val language: String,
        @ProtoNumber(4) val homeUrl: String = "",
        @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
        @ProtoNumber(7) val message: String? = null,
    )

    @Suppress("Unused")
    enum class ContentWarning {
        @ProtoNumber(0)
        @JsonNames("CONTENT_WARNING_UNSPECIFIED")
        UNSPECIFIED,

        @ProtoNumber(1)
        @JsonNames("CONTENT_WARNING_SAFE")
        SAFE,

        @ProtoNumber(2)
        @JsonNames("CONTENT_WARNING_MIXED")
        MIXED,

        @ProtoNumber(3)
        @JsonNames("CONTENT_WARNING_NSFW")
        NSFW,
    }

    fun toExtensionRepo(baseUrl: String): ExtensionRepo {
        return ExtensionRepo(
            baseUrl = baseUrl,
            name = name,
            shortName = badgeLabel,
            website = contact.website,
            signingKeyFingerprint = signingKey,
        )
    }
}
