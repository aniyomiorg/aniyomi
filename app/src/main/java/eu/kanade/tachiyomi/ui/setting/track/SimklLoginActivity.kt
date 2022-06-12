package eu.kanade.tachiyomi.ui.setting.track

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.util.lang.launchIO

class SimklLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(data: Uri?) {
        val code = data?.getQueryParameter("code")
        if (code != null) {
            lifecycleScope.launchIO {
                trackManager.simkl.login(code)
                returnToSettings()
            }
        } else {
            trackManager.simkl.logout()
            returnToSettings()
        }
    }
}
