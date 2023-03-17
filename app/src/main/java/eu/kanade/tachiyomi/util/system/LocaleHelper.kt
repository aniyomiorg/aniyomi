package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.core.os.LocaleListCompat
import eu.kanade.tachiyomi.R
import java.util.Locale

/**
 * Utility class to change the application's language in runtime.
 */
object LocaleHelper {

    val comparator = compareBy<String>(
        { getDisplayName(it) },
        { it == "all" },
    )

    /**
     * Returns display name of a string language code.
     */
    fun getSourceDisplayName(lang: String?, context: Context): String {
        return when (lang) {
            LAST_USED_KEY -> context.getString(R.string.last_used_source)
            PINNED_KEY -> context.getString(R.string.pinned_sources)
            "other" -> context.getString(R.string.other_source)
            "all" -> context.getString(R.string.multi_lang)
            else -> getDisplayName(lang)
        }
    }

    /**
     * Returns display name of a string language code.
     *
     * @param lang empty for system language
     */
    fun getDisplayName(lang: String?): String {
        if (lang == null) {
            return ""
        }

        val locale = when (lang) {
            "" -> LocaleListCompat.getAdjustedDefault()[0]
            "zh-CN" -> Locale.forLanguageTag("zh-Hans")
            "zh-TW" -> Locale.forLanguageTag("zh-Hant")
            else -> Locale.forLanguageTag(lang)
        }
        return locale!!.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }

    /**
     * Return the default languages enabled for the sources.
     */
    fun getDefaultEnabledLanguages(): Set<String> {
        return setOf("all", "en", Locale.getDefault().language)
    }

    /**
     * Return English display string from string language code
     */
    fun getSimpleLocaleDisplayName(): String {
        val sp = Locale.getDefault().language.split("_", "-")
        return Locale(sp[0]).getDisplayLanguage(LocaleListCompat.getDefault()[0]!!)
    }
}

internal const val PINNED_KEY = "pinned"
internal const val LAST_USED_KEY = "last_used"
