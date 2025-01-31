package eu.kanade.tachiyomi.ui.player.cast

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.media.widget.MiniControllerFragment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.PlayerActivity

@Composable
fun CastMiniController(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var castState by remember { mutableIntStateOf(CastState.NO_DEVICES_AVAILABLE) }
    val playerActivity = context as? PlayerActivity

    LaunchedEffect(playerActivity) {
        val castContext = playerActivity?.castManager?.castContext ?: return@LaunchedEffect
        // Update state with current value and listen for changes
        castState = castContext.castState
        castContext.addCastStateListener { state ->
            castState = state
        }
    }

    if (castState == CastState.CONNECTED) {
        AndroidView(
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = R.id.castMiniController
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                }
            },
            update = { view ->
                val fragment = MiniControllerFragment()
                val fragmentManager = (context as FragmentActivity).supportFragmentManager
                fragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitNowAllowingStateLoss()
            },
            modifier = modifier,
        )
    }
}
