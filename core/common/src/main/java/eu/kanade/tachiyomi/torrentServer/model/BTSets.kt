package eu.kanade.tachiyomi.torrentServer.model

import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming")
data class BTSets(
    // Cache
    var CacheSize: Long,
    var PreloadBuffer: Boolean,
    var PreloadCache: Int,
    var ReaderReadAHead: Int,
    // Storage
    var UseDisk: Boolean,
    var TorrentsSavePath: String,
    var RemoveCacheOnDrop: Boolean,
    // Torrent
    var ForceEncrypt: Boolean,
    var RetrackersMode: Int,
    var TorrentDisconnectTimeout: Int,
    var EnableDebug: Boolean,
    // DLNA
    var EnableDLNA: Boolean,
    var FriendlyName: String,
    // Rutor search
    var EnableRutorSearch: Boolean,
    // BT Config
    var EnableIPv6: Boolean,
    var DisableTCP: Boolean,
    var DisableUTP: Boolean,
    var DisableUPNP: Boolean,
    var DisableDHT: Boolean,
    var DisablePEX: Boolean,
    var DisableUpload: Boolean,
    var DownloadRateLimit: Int,
    var UploadRateLimit: Int,
    var ConnectionsLimit: Int,
    var DhtConnectionLimit: Int,
    var PeersListenPort: Int,
)
