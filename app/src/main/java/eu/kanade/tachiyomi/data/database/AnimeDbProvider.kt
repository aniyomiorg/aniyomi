package eu.kanade.tachiyomi.data.database

import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite

interface AnimeDbProvider {

    val db: DefaultStorIOSQLite
}
