package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

val topSmallPaddingValues = PaddingValues(top = MaterialTheme.padding.small)

const val DISABLED_ALPHA = .38f
const val SECONDARY_ALPHA = .78f

class Padding {

    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val mediumSmall = 12.dp

    val small = 8.dp

    val extraSmall = 4.dp
}

val MaterialTheme.padding: Padding
    get() = Padding()

// Used to make merging from mpvkt easier
class MPVKtSpacing {
    val extraSmall = 4.dp

    val smaller = 8.dp

    val small = 12.dp

    val medium = 16.dp

    val large = 24.dp

    val larger = 32.dp

    val extraLarge = 48.dp

    val largest = 64.dp
}

val MaterialTheme.MPVKtSpacing: MPVKtSpacing
    get() = MPVKtSpacing()
