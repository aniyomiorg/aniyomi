package aniyomi.core.common.torrent

import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class TorrentPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun torrServerEnable() = preferenceStore.getBoolean("pref_torrserver_enable", false)
    fun torrServerShownNotice() = preferenceStore.getBoolean("pref_torrserver_shownotice", false)

    fun torrServerPort() = preferenceStore.getString("pref_torrserver_port", "8090")
    fun torrServerTrackers() = preferenceStore.getString(
        "pref_torrserver_tackers",
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
    fun torrServerProxyMode() = preferenceStore.getEnum("pref_torrserver_proxymode", ProxyMode.None)
    fun torrServerProxyUrl() = preferenceStore.getString("pref_torrserver_proxyurl", "")
}

enum class ProxyMode(val value: String) {
    None("tracker"),
    Tracker("tracker"),
    Peers("peers"),
    Full("full"),
}
