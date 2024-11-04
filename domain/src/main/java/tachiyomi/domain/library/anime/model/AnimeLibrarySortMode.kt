package tachiyomi.domain.library.anime.model

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.FlagWithMask
import tachiyomi.domain.library.model.plus

data class AnimeLibrarySort(
    val type: Type,
    val direction: Direction,
) : FlagWithMask {

    override val flag: Long
        get() = type + direction

    override val mask: Long
        get() = type.mask or direction.mask

    val isAscending: Boolean
        get() = direction == Direction.Ascending

    sealed class Type(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = 0b00111100L

        data object Alphabetical : Type(0b00000000)
        data object LastSeen : Type(0b00000100)
        data object LastUpdate : Type(0b00001000)
        data object UnseenCount : Type(0b00001100)
        data object TotalEpisodes : Type(0b00010000)
        data object LatestEpisode : Type(0b00010100)
        data object EpisodeFetchDate : Type(0b00011000)
        data object DateAdded : Type(0b00011100)
        data object TrackerMean : Type(0b000100000)
        data object AiringTime : Type(0b00110000)
        data object Random : Type(0b00111100)

        companion object {
            fun valueOf(flag: Long): Type {
                return types.find { type -> type.flag == flag and type.mask } ?: default.type
            }
        }
    }

    sealed class Direction(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = 0b01000000L

        data object Ascending : Direction(0b01000000)
        data object Descending : Direction(0b00000000)

        companion object {
            fun valueOf(flag: Long): Direction {
                return directions.find { direction -> direction.flag == flag and direction.mask } ?: default.direction
            }
        }
    }

    object Serializer {
        fun deserialize(serialized: String): AnimeLibrarySort {
            return Companion.deserialize(serialized)
        }

        fun serialize(value: AnimeLibrarySort): String {
            return value.serialize()
        }
    }

    companion object {
        val types by lazy {
            setOf(
                Type.Alphabetical,
                Type.LastSeen,
                Type.LastUpdate,
                Type.UnseenCount,
                Type.TotalEpisodes,
                Type.LatestEpisode,
                Type.EpisodeFetchDate,
                Type.DateAdded,
                Type.TrackerMean,
                Type.AiringTime,
                Type.Random,
            )
        }
        val directions by lazy { setOf(Direction.Ascending, Direction.Descending) }
        val default = AnimeLibrarySort(Type.Alphabetical, Direction.Ascending)

        fun valueOf(flag: Long?): AnimeLibrarySort {
            if (flag == null) return default
            return AnimeLibrarySort(
                Type.valueOf(flag),
                Direction.valueOf(flag),
            )
        }

        fun deserialize(serialized: String): AnimeLibrarySort {
            if (serialized.isEmpty()) return default
            return try {
                val values = serialized.split(",")
                val type = when (values[0]) {
                    "ALPHABETICAL" -> Type.Alphabetical
                    "LAST_SEEN" -> Type.LastSeen
                    "LAST_ANIME_UPDATE" -> Type.LastUpdate
                    "UNSEEN_COUNT" -> Type.UnseenCount
                    "TOTAL_EPISODES" -> Type.TotalEpisodes
                    "LATEST_EPISODE" -> Type.LatestEpisode
                    "EPISODE_FETCH_DATE" -> Type.EpisodeFetchDate
                    "DATE_ADDED" -> Type.DateAdded
                    "TRACKER_MEAN" -> Type.TrackerMean
                    "AIRING_TIME" -> Type.AiringTime
                    "RANDOM" -> Type.Random
                    else -> Type.Alphabetical
                }
                val ascending = if (values[1] == "ASCENDING") Direction.Ascending else Direction.Descending
                AnimeLibrarySort(type, ascending)
            } catch (e: Exception) {
                default
            }
        }
    }

    fun serialize(): String {
        val type = when (type) {
            Type.Alphabetical -> "ALPHABETICAL"
            Type.LastSeen -> "LAST_SEEN"
            Type.LastUpdate -> "LAST_ANIME_UPDATE"
            Type.UnseenCount -> "UNSEEN_COUNT"
            Type.TotalEpisodes -> "TOTAL_EPISODES"
            Type.LatestEpisode -> "LATEST_EPISODE"
            Type.EpisodeFetchDate -> "EPISODE_FETCH_DATE"
            Type.DateAdded -> "DATE_ADDED"
            Type.TrackerMean -> "TRACKER_MEAN"
            Type.AiringTime -> "AIRING_TIME"
            Type.Random -> "RANDOM"
        }
        val direction = if (direction == Direction.Ascending) "ASCENDING" else "DESCENDING"
        return "$type,$direction"
    }
}

val Category?.sort: AnimeLibrarySort
    get() = AnimeLibrarySort.valueOf(this?.flags)
