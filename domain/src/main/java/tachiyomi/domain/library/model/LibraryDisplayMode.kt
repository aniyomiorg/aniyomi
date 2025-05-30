package tachiyomi.domain.library.model

sealed interface LibraryDisplayMode {

    data object CompactGrid : LibraryDisplayMode
    data object ComfortableGrid : LibraryDisplayMode
    data object List : LibraryDisplayMode
    data object CoverOnlyGrid : LibraryDisplayMode

    // KMK -->
    data object ComfortableGridPanorama : LibraryDisplayMode
    // KMK <--

    object Serializer {
        fun deserialize(serialized: String): LibraryDisplayMode {
            return Companion.deserialize(serialized)
        }

        fun serialize(value: LibraryDisplayMode): String {
            return value.serialize()
        }
    }

    companion object {
        val values by lazy { setOf(CompactGrid, ComfortableGrid, List, CoverOnlyGrid) }
        val default = CompactGrid

        fun deserialize(serialized: String): LibraryDisplayMode {
            return when (serialized) {
                "COMFORTABLE_GRID" -> ComfortableGrid
                "COMPACT_GRID" -> CompactGrid
                "COVER_ONLY_GRID" -> CoverOnlyGrid
                "LIST" -> List
                else -> default
            }
        }
    }

    fun serialize(): String {
        return when (this) {
            ComfortableGrid -> "COMFORTABLE_GRID"
            // KMK -->
            ComfortableGridPanorama -> "COMFORTABLE_GRID_PANORAMA"
            // KMK <--
            CompactGrid -> "COMPACT_GRID"
            CoverOnlyGrid -> "COVER_ONLY_GRID"
            List -> "LIST"
        }
    }
}
