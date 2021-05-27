package eu.kanade.tachiyomi.ui.browse.animesource.filter

import eu.davidea.flexibleadapter.items.ISectionable
import eu.kanade.tachiyomi.animesource.model.AnimeFilter

class TriStateSectionItem(filter: AnimeFilter.TriState) : TriStateItem(filter), ISectionable<TriStateItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TriStateSectionItem
        if (head != other.head) return false
        return filter == other.filter
    }

    override fun hashCode(): Int {
        return filter.hashCode() + (head?.hashCode() ?: 0)
    }
}

class TextSectionItem(filter: AnimeFilter.Text) : TextItem(filter), ISectionable<TextItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextSectionItem
        if (head != other.head) return false
        return filter == other.filter
    }

    override fun hashCode(): Int {
        return filter.hashCode() + (head?.hashCode() ?: 0)
    }
}

class CheckboxSectionItem(filter: AnimeFilter.CheckBox) : CheckboxItem(filter), ISectionable<CheckboxItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CheckboxSectionItem
        if (head != other.head) return false
        return filter == other.filter
    }

    override fun hashCode(): Int {
        return filter.hashCode() + (head?.hashCode() ?: 0)
    }
}

class SelectSectionItem(filter: AnimeFilter.Select<*>) : SelectItem(filter), ISectionable<SelectItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectSectionItem
        if (head != other.head) return false
        return filter == other.filter
    }

    override fun hashCode(): Int {
        return filter.hashCode() + (head?.hashCode() ?: 0)
    }
}
