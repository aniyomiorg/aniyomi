package eu.kanade.domain.ui.model

import eu.kanade.tachiyomi.R

enum class AppTheme(val titleResId: Int?) {
    DEFAULT(R.string.label_default),
    MONET(R.string.theme_monet),
    COTTONCANDY(R.string.theme_cottoncandy),
    GREEN_APPLE(R.string.theme_greenapple),
    LAVENDER(R.string.theme_lavender),
    MIDNIGHT_DUSK(R.string.theme_midnightdusk),
    MOCHA(R.string.theme_mocha),
    STRAWBERRY_DAIQUIRI(R.string.theme_strawberrydaiquiri),
    TAKO(R.string.theme_tako),
    TEALTURQUOISE(R.string.theme_tealturquoise),
    TIDAL_WAVE(R.string.theme_tidalwave),
    YINYANG(R.string.theme_yinyang),
    YOTSUBA(R.string.theme_yotsuba),
    CLOUDFLARE(R.string.theme_cloudflare),
    SAPPHIRE(R.string.theme_sapphire),
    DOOM(R.string.theme_doom),
    MATRIX(R.string.theme_matrix),

    // Deprecated
    DARK_BLUE(null),
    HOT_PINK(null),
    BLUE(null),
}
