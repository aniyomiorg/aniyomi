package eu.kanade.tachiyomi.torrentServer

import tachiyomi.core.common.preference.PreferenceStore

class TorrentServerPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun port() = preferenceStore.getString("pref_torrent_port", "8090")

    fun trackers() = preferenceStore.getString(
        "pref_torrent_trackers",
        """http://nyaa.tracker.wf:7777/announce
           http://anidex.moe:6969/announce
           http://tracker.anirena.com:80/announce
           udp://tracker.uw0.xyz:6969/announce
           http://share.camoe.cn:8080/announce
           http://t.nyaatracker.com:80/announce
           udp://47.ip-51-68-199.eu:6969/announce
           udp://9.rarbg.me:2940
           udp://9.rarbg.to:2820
           udp://exodus.desync.com:6969/announce
           udp://explodie.org:6969/announce
           udp://ipv4.tracker.harry.lu:80/announce
           udp://open.stealth.si:80/announce
           udp://opentor.org:2710/announce
           udp://opentracker.i2p.rocks:6969/announce
           udp://retracker.lanta-net.ru:2710/announce
           udp://tracker.cyberia.is:6969/announce
           udp://tracker.dler.org:6969/announce
           udp://tracker.ds.is:6969/announce
           udp://tracker.internetwarriors.net:1337
           udp://tracker.openbittorrent.com:6969/announce
           udp://tracker.opentrackr.org:1337/announce
           udp://tracker.tiny-vps.com:6969/announce
           udp://tracker.torrent.eu.org:451/announce
           udp://valakas.rollo.dnsabr.com:2710/announce
           udp://www.torrent.eu.org:451/announce""".replace(" ", ""),
    )
}
