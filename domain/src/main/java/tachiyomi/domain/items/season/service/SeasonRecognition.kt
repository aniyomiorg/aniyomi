package tachiyomi.domain.items.season.service

/**
 * -R> = regex conversion.
 */
object SeasonRecognition {

    private const val NUMBER_PATTERN = """([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?"""

    /**
     * All cases with s.xx, s xx, season xx, or sxx
     * Boku.no.Hero.Academia.S02.1080p-ITH -R> 2
     */
    private val basic = Regex("""(?<=\bs\.|\bs|season) *$NUMBER_PATTERN""")

    /**
     * Example: Boku no Hero Academia 2 -R> 2
     */
    private val number = Regex(NUMBER_PATTERN)

    /**
     * Regex to remove tags
     * Example: [FLE] Boku no Hero Academia Season 2 (BD Remux 1080p H.264 FLAC) [Dual Audio]
     */
    private val tagRegex = Regex("""^\[[^\]]+\]|\[[^\]]+\]\s*${'$'}|^\([^\)]+\)|\([^\)]+\)\s*${'$'}""")

    /**
     * Regex used to remove unwanted qualities and year
     * Example: Boku no Hero Academia (2017)
     */
    private val unwanted = Regex("""\b\d+p\b|\d+x\d+|Hi10|\(\d+\)""")

    /**
     * Regex used to remove unwanted whitespace
     * Example One Piece 12 special -R> One Piece 12special
     */
    private val unwantedWhiteSpace = Regex("""\s(?=extra|special|omake)""")

    fun parseSeasonNumber(animeTitle: String, seasonName: String, seasonNumber: Double? = null): Double {
        // If season number is known return.
        if (seasonNumber != null && (seasonNumber == -2.0 || seasonNumber > -1.0)) {
            return seasonNumber
        }

        // Get season title with lower case
        var cleanSeasonName = seasonName.lowercase()
            // Remove anime title from season title.
            .replace(animeTitle.lowercase(), "").trim()
            // Remove comma's or hyphens.
            .replace(',', '.')
            .replace('-', '.')
            // Remove unwanted white spaces.
            .replace(unwantedWhiteSpace, "")

        // Remove all tags while they exist
        while (tagRegex.containsMatchIn(cleanSeasonName)) {
            cleanSeasonName = tagRegex.replace(cleanSeasonName, "")
        }

        val numberMatch = number.findAll(cleanSeasonName)

        when {
            numberMatch.none() -> {
                return seasonNumber ?: -1.0
            }
            numberMatch.count() > 1 -> {
                // Remove unwanted tags.
                unwanted.replace(cleanSeasonName, "").let { name ->
                    // Check base case s.xx
                    basic.find(name)?.let { return getSeasonNumberFromMatch(it) }

                    // need to find again first number might already removed
                    number.find(name)?.let { return getSeasonNumberFromMatch(it) }
                }
            }
        }

        return getSeasonNumberFromMatch(numberMatch.first())
    }

    /**
     * Check if season number is found and return it
     * @param match result of regex
     * @return season number if found else null
     */
    private fun getSeasonNumberFromMatch(match: MatchResult): Double {
        return match.let {
            val initial = it.groups[1]?.value?.toDouble()!!
            val subSeasonDecimal = it.groups[2]?.value
            val subSeasonAlpha = it.groups[3]?.value
            val addition = checkForDecimal(subSeasonDecimal, subSeasonAlpha)
            initial.plus(addition)
        }
    }

    /**
     * Check for decimal in received strings
     * @param decimal decimal value of regex
     * @param alpha alpha value of regex
     * @return decimal/alpha float value
     */
    private fun checkForDecimal(decimal: String?, alpha: String?): Double {
        if (!decimal.isNullOrEmpty()) {
            return decimal.toDouble()
        }

        if (!alpha.isNullOrEmpty()) {
            if (alpha.contains("extra")) {
                return 0.99
            }

            if (alpha.contains("omake")) {
                return 0.98
            }

            if (alpha.contains("special")) {
                return 0.97
            }

            val trimmedAlpha = alpha.trimStart('.')
            if (trimmedAlpha.length == 1) {
                return parseAlphaPostFix(trimmedAlpha[0])
            }
        }

        return 0.0
    }

    /**
     * x.a -> x.1, x.b -> x.2, etc
     */
    private fun parseAlphaPostFix(alpha: Char): Double {
        val number = alpha.code - ('a'.code - 1)
        if (number >= 10) return 0.0
        return number / 10.0
    }
}
