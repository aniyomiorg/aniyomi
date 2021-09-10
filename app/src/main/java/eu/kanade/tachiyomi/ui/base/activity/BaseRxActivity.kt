package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
<<<<<<< HEAD
import eu.kanade.tachiyomi.util.system.LocaleHelper
=======
import eu.kanade.tachiyomi.util.system.prepareTabletUiContext
>>>>>>> upstream/master
import nucleus.view.NucleusAppCompatActivity

abstract class BaseRxActivity<VB : ViewBinding, P : BasePresenter<*>> : NucleusAppCompatActivity<P>() {

    @Suppress("LeakingThis")
    private val secureActivityDelegate = SecureActivityDelegate(this)

    lateinit var binding: VB

    override fun attachBaseContext(newBase: Context) {
<<<<<<< HEAD
        super.attachBaseContext(LocaleHelper.createLocaleWrapper(newBase))
=======
        super.attachBaseContext(newBase.prepareTabletUiContext())
>>>>>>> upstream/master
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        secureActivityDelegate.onCreate()
    }

    override fun onResume() {
        super.onResume()

        secureActivityDelegate.onResume()
    }
}
