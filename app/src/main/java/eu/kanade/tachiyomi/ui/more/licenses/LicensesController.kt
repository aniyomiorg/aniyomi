package eu.kanade.tachiyomi.ui.more.licenses

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.toStringArray
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.LicensesControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.openInBrowser

class LicensesController :
    BaseController<LicensesControllerBinding>(),
    FlexibleAdapter.OnItemClickListener {

    private var adapter: LicensesAdapter? = null

    override fun getTitle(): String? {
        return resources?.getString(R.string.licenses)
    }

    override fun createBinding(inflater: LayoutInflater) = LicensesControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }
        binding.progress.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        adapter = LicensesAdapter(this)
        binding.recycler.adapter = adapter

        viewScope.launchUI {
            val licenseItems = withIOContext {
                val fields = resolveTachiRClass()?.fields?.toStringArray()
                    ?: return@withIOContext null
                Libs(view.context, fields).libraries
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.libraryName }))
                    .map { LicensesItem(it) }
            } ?: return@launchUI
            binding.progress.hide()
            adapter?.updateDataSet(licenseItems)
        }
    }

    private fun resolveTachiRClass(): Class<*>? {
        var packageName = "eu.kanade.tachiyomi"
        do {
            try {
                return Class.forName("$packageName.R\$string")
            } catch (e: ClassNotFoundException) {
                packageName = if (packageName.contains(".")) packageName.substring(0, packageName.lastIndexOf('.')) else ""
            }
        } while (packageName.isNotEmpty())

        return null
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val adapter = adapter ?: return false

        val item = adapter.getItem(position) ?: return false
        openLicenseWebsite(item)
        return true
    }

    private fun openLicenseWebsite(item: LicensesItem) {
        val website = item.library.libraryWebsite
        if (website.isNotEmpty()) {
            activity?.openInBrowser(website)
        }
    }
}
