package eu.kanade.tachiyomi.widget.sheet

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.databinding.CommonTabbedSheetBinding
import eu.kanade.tachiyomi.widget.ViewPagerAdapter

abstract class TabbedPlayerBottomSheetDialog(private val activity: Activity) : PlayerBottomSheetDialog(activity) {

    lateinit var binding: CommonTabbedSheetBinding

    override fun createView(inflater: LayoutInflater): View {
        binding = CommonTabbedSheetBinding.inflate(activity.layoutInflater)

        val adapter = LibrarySettingsSheetAdapter()
        binding.pager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.pager)

        return binding.root
    }

    abstract fun getTabs(): List<Pair<View, Int>>

    private inner class LibrarySettingsSheetAdapter : ViewPagerAdapter() {

        override fun createView(container: ViewGroup, position: Int): View {
            return getTabs()[position].first
        }

        override fun getCount(): Int {
            return getTabs().size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return activity.resources!!.getString(getTabs()[position].second)
        }
    }
}
