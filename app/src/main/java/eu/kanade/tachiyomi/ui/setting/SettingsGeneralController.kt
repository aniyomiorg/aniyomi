package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.preference.bindTo
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
import java.util.Date
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsGeneralController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_general

        intListPreference {
            key = Keys.startScreen
            titleRes = R.string.pref_start_screen
            entriesRes = when (preferences.bottomNavStyle()) {
                1 -> startScreenArrayHistory
                2 -> startScreenArrayNoManga
                else -> startScreenArrayDefault
            }
            entryValues = arrayOf("1", "2", "3", "4")
            defaultValue = "1"
            summary = "%s"
        }

        intListPreference {
            key = Keys.bottomNavStyle
            titleRes = R.string.pref_bottom_nav_style
            entriesRes = arrayOf(
                R.string.label_default,
                R.string.pref_bottom_nav_history,
                R.string.pref_bottom_nav_no_manga,
            )
            entryValues = arrayOf("0", "1", "2")
            defaultValue = "0"
            summary = "%s"
            onChange {
                activity?.let { ActivityCompat.recreate(it) }
                true
            }
        }

        switchPreference {
            bindTo(preferences.showUpdatesNavBadge())
            titleRes = R.string.pref_library_update_show_tab_badge
        }
        switchPreference {
            key = Keys.confirmExit
            titleRes = R.string.pref_confirm_exit
            defaultValue = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            preference {
                key = "pref_manage_notifications"
                titleRes = R.string.pref_manage_notifications
                onClick {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    startActivity(intent)
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_locale

            listPreference {
                bindTo(preferences.lang())
                titleRes = R.string.pref_language

                val langs = mutableListOf<Pair<String, String>>()
                langs += Pair(
                    "",
                    "${context.getString(R.string.system_default)} (${LocaleHelper.getDisplayName("")})",
                )
                // Due to compatibility issues:
                // - Hebrew: `he` is copied into `iw` at build time
                langs += arrayOf(
                    "am",
                    "ar",
                    "be",
                    "bg",
                    "bn",
                    "ca",
                    "cs",
                    "cv",
                    "de",
                    "el",
                    "eo",
                    "es",
                    "es-419",
                    "en",
                    "fa",
                    "fi",
                    "fil",
                    "fr",
                    "gl",
                    "he",
                    "hi",
                    "hr",
                    "hu",
                    "in",
                    "it",
                    "ja",
                    "jv",
                    "ka-rGE",
                    "kn",
                    "ko",
                    "lt",
                    "lv",
                    "mr",
                    "ms",
                    "my",
                    "nb-rNO",
                    "ne",
                    "nl",
                    "pl",
                    "pt",
                    "pt-BR",
                    "ro",
                    "ru",
                    "sah",
                    "sc",
                    "sk",
                    "sr",
                    "sv",
                    "te",
                    "th",
                    "tr",
                    "uk",
                    "ur-rPK",
                    "vi",
                    "uz",
                    "zh-rCN",
                    "zh-rTW",
                )
                    .map {
                        Pair(it, LocaleHelper.getDisplayName(it))
                    }
                    .sortedBy { it.second }

                entryValues = langs.map { it.first }.toTypedArray()
                entries = langs.map { it.second }.toTypedArray()
                defaultValue = ""
                summary = "%s"

                onChange {
                    activity?.recreate()
                    true
                }
            }
            listPreference {
                key = Keys.dateFormat
                titleRes = R.string.pref_date_format
                entryValues = arrayOf("", "MM/dd/yy", "dd/MM/yy", "yyyy-MM-dd", "dd MMM yyyy", "MMM dd, yyyy")

                val now = Date().time
                entries = entryValues.map { value ->
                    val formattedDate = preferences.dateFormat(value.toString()).format(now)
                    if (value == "") {
                        "${context.getString(R.string.system_default)} ($formattedDate)"
                    } else {
                        "$value ($formattedDate)"
                    }
                }.toTypedArray()

                defaultValue = ""
                summary = "%s"
            }
        }
    }
}

private val startScreenArrayDefault = arrayOf(
    R.string.label_animelib,
    R.string.label_manga,
    R.string.label_recent_updates,
    R.string.browse,
)

private val startScreenArrayHistory = arrayOf(
    R.string.label_animelib,
    R.string.label_manga,
    R.string.label_recent_manga,
    R.string.browse,
)

private val startScreenArrayNoManga = arrayOf(
    R.string.label_animelib,
    R.string.label_recent_updates,
    R.string.label_recent_manga,
    R.string.browse,
)
