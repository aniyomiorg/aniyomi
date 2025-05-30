package eu.kanade.tachiyomi.ui.player.cast

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
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

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle back button to minimize instead of disconnecting
        onBackPressedDispatcher.addCallback(this) {
            minimizePlayer()
        }
        overridePendingTransition(
            R.anim.slide_in_up,
            R.anim.player_fade_out,
        )

        setContent {
            TachiyomiTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ExpandedControllerScreen(
                        castManager = castManager,
                        castContext = castContext,
                        onBackPressed = { minimizePlayer() },
                        navigationIcon = {
                            IconButton(onClick = { minimizePlayer() }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Minimize",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun minimizePlayer() {
        finish()
        overridePendingTransition(
            R.anim.fade_in,
            R.anim.slide_out_down,
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }
}
