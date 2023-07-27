package aniyomi.md.manga.dto

import aniyomi.md.dto.ListCallDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MangaListDto(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<MangaDataDto>,
) : ListCallDto<MangaDataDto>

@Serializable
data class MangaDto(
    val result: String,
    val data: MangaDataDto,
)

@Serializable
data class MangaDataDto(
    val id: String,
    val type: String,
    val attributes: MangaAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class MangaAttributesDto(
    val title: JsonElement,
    val altTitles: List<Map<String, String>>,
    val description: JsonElement,
    val links: JsonElement?,
    val originalLanguage: String,
    val lastVolume: String?,
    val lastChapter: String?,
    val contentRating: String?,
    val publicationDemographic: String?,
    val status: String?,
    val year: Int?,
    val tags: List<TagDto>,
)

@Serializable
data class TagDto(
    val id: String,
    val attributes: TagAttributesDto,
)

@Serializable
data class TagAttributesDto(
    val name: Map<String, String>,
)

@Serializable
data class RelationshipDto(
    val id: String,
    val type: String,
    val attributes: IncludesAttributesDto? = null,
)

@Serializable
data class IncludesAttributesDto(
    val name: String? = null,
    val fileName: String? = null,
)
