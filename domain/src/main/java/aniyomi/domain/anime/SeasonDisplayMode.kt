package aniyomi.domain.anime

sealed interface SeasonDisplayMode {
    data object CompactGrid : SeasonDisplayMode
    data object ComfortableGrid : SeasonDisplayMode
    data object CoverOnlyGrid : SeasonDisplayMode
    data object List : SeasonDisplayMode

    companion object {
        fun toLong(value: SeasonDisplayMode): Long {
            return when (value) {
                CompactGrid -> 0L
                ComfortableGrid -> 1L
                CoverOnlyGrid -> 2L
                List -> 3L
            }
        }

        fun fromLong(value: Long): SeasonDisplayMode {
            return when (value) {
                0L -> CompactGrid
                1L -> ComfortableGrid
                2L -> CoverOnlyGrid
                3L -> List
                else -> throw IllegalArgumentException("Invalid sorting value")
            }
        }
    }
}
