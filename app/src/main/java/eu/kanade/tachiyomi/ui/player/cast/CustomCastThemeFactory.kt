package eu.kanade.tachiyomi.ui.player.cast

import android.content.Context
import android.os.Bundle
import androidx.mediarouter.app.MediaRouteActionProvider
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.app.MediaRouteChooserDialogFragment
import androidx.mediarouter.app.MediaRouteControllerDialog
import androidx.mediarouter.app.MediaRouteControllerDialogFragment
import androidx.mediarouter.app.MediaRouteDialogFactory
import eu.kanade.tachiyomi.R

class CustomCastProvider(context: Context) : MediaRouteActionProvider(context) {
    init {
        dialogFactory = CustomCastThemeFactory()
    }
}

class CustomCastThemeFactory : MediaRouteDialogFactory() {
    override fun onCreateChooserDialogFragment(): MediaRouteChooserDialogFragment {
        return CustomMediaRouterChooserDialogFragment()
    }

    override fun onCreateControllerDialogFragment(): MediaRouteControllerDialogFragment {
        return CustomMediaRouteControllerDialogFragment()
    }
}

class CustomMediaRouterChooserDialogFragment : MediaRouteChooserDialogFragment() {
    override fun onCreateChooserDialog(
        context: Context,
        savedInstanceState: Bundle?,
    ): MediaRouteChooserDialog {
        return MediaRouteChooserDialog(context, R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
    }
}

class CustomMediaRouteControllerDialogFragment : MediaRouteControllerDialogFragment() {
    override fun onCreateControllerDialog(
        context: Context,
        savedInstanceState: Bundle?,
    ): MediaRouteControllerDialog {
        return MediaRouteControllerDialog(context, R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
    }
}
