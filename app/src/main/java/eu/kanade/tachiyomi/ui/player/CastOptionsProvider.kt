package eu.kanade.tachiyomi.ui.player

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import eu.kanade.tachiyomi.R

class CastOptionsProvider : OptionsProvider {
    @SuppressLint("VisibleForTests")
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.app_cast_id))
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
