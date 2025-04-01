package eu.kanade.tachiyomi.data.track.bangumi.dto

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class BGMSubject(
    val images: BGMSearchItemCovers?,
    val summary: String,
    val name: String,
    @SerialName("name_cn")
    val nameCn: String,
    val infobox: List<Infobox>,
    val id: Long,
)

// infobox deserializer and related classes courtesy of
// https://github.com/Snd-R/komf/blob/4c260a3dcd326a5e1d74ac9662eec8124ab7e461/komf-core/src/commonMain/kotlin/snd/komf/providers/bangumi/model/BangumiSubject.kt#L53-L89
object InfoBoxSerializer : JsonContentPolymorphicSerializer<Infobox>(Infobox::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Infobox> {
        if (element !is JsonObject) throw SerializationException("Expected JsonObject go ${element::class}")
        val value = element["value"]

        return when (value) {
            is JsonArray -> Infobox.MultipleValues.serializer()
            is JsonPrimitive -> Infobox.SingleValue.serializer()
            else -> throw SerializationException("Unexpected element type ${element::class}")
        }
    }
}

@Serializable(with = InfoBoxSerializer::class)
sealed interface Infobox {
    val key: String

    @Serializable
    class SingleValue(
        override val key: String,
        val value: String,
    ) : Infobox

    @Serializable
    class MultipleValues(
        override val key: String,
        val value: List<InfoboxNestedValue>,
    ) : Infobox
}

@Serializable
data class InfoboxNestedValue(
    @SerialName("k")
    val key: String? = null,
    @SerialName("v")
    val value: String,
)
