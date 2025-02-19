package eu.kanade.tachiyomi.ui.player.cast

import android.os.Bundle
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.ui.player.cast.components.ExpandedControllerScreen
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.injectLazy

class ExpandedControlsActivity : ComponentActivity() {
    private val preferences: PreferenceStore by injectLazy()
    private val castManager by lazy { CastManager(this, preferences) }
    private val castContext by lazy { CastContext.getSharedInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TachiyomiTheme {
                ExpandedControllerScreen(
                    castManager = castManager,
                    castContext = castContext,
                    onBackPressed = { finish() },
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }
}
