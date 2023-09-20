package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Activity
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            val resIds = mutableListOf<Int>()
            when (appTheme) {
                AppTheme.MONET -> {
                    resIds += R.style.Theme_Tachiyomi_Monet
                }
                AppTheme.COTTONCANDY -> {
                    resIds += R.style.Theme_Tachiyomi_CottonCandy
                }
                AppTheme.GREEN_APPLE -> {
                    resIds += R.style.Theme_Tachiyomi_GreenApple
                }
                AppTheme.LAVENDER -> {
                    resIds += R.style.Theme_Tachiyomi_Lavender
                }
                AppTheme.MIDNIGHT_DUSK -> {
                    resIds += R.style.Theme_Tachiyomi_MidnightDusk
                }
                AppTheme.MOCHA -> {
                    resIds += R.style.Theme_Tachiyomi_Mocha
                }
                AppTheme.STRAWBERRY_DAIQUIRI -> {
                    resIds += R.style.Theme_Tachiyomi_StrawberryDaiquiri
                }
                AppTheme.TAKO -> {
                    resIds += R.style.Theme_Tachiyomi_Tako
                }
                AppTheme.TEALTURQUOISE -> {
                    resIds += R.style.Theme_Tachiyomi_TealTurquoise
                }
                AppTheme.YINYANG -> {
                    resIds += R.style.Theme_Tachiyomi_YinYang
                }
                AppTheme.YOTSUBA -> {
                    resIds += R.style.Theme_Tachiyomi_Yotsuba
                }
                AppTheme.CLOUDFLARE -> {
                    resIds += R.style.Theme_Tachiyomi_Cloudflare
                }
                AppTheme.SAPPHIRE -> {
                    resIds += R.style.Theme_Tachiyomi_Sapphire
                }
                AppTheme.DOOM -> {
                    resIds += R.style.Theme_Tachiyomi_Doom
                }
                AppTheme.MATRIX -> {
                    resIds += R.style.Theme_Tachiyomi_Matrix
                }
                AppTheme.TIDAL_WAVE -> {
                    resIds += R.style.Theme_Tachiyomi_TidalWave
                }
                else -> {
                    resIds += R.style.Theme_Tachiyomi
                }
            }

            if (isAmoled) {
                resIds += R.style.ThemeOverlay_Tachiyomi_Amoled
            }

            return resIds
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val uiPreferences = Injekt.get<UiPreferences>()
        ThemingDelegate.getThemeResIds(uiPreferences.appTheme().get(), uiPreferences.themeDarkAmoled().get())
            .forEach(activity::setTheme)
    }
}
