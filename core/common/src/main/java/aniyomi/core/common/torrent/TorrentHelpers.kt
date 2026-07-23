package aniyomi.core.common.torrent

import aniyomi.core.common.torrent.bencode.BencodeParser
import aniyomi.core.common.torrent.bencode.BencodeValue
import aniyomi.core.common.torrent.bencode.BencodeWriter
import aniyomi.core.common.torrent.model.FileStats
import aniyomi.core.common.torrent.model.Torrent
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest

object TorrentHelpers {
    fun parseTorrentDetailsFromTorrentFileContent(torrentFileContent: InputStream): Torrent {
        try {
            val parsed = BencodeParser.parse(torrentFileContent) as BencodeValue.Dictionary
            val infoDictionary = parsed.getByString("info") as BencodeValue.Dictionary

            val title = (infoDictionary.getByString("name") as BencodeValue.ByteString).toUTF8String()
            val hash = calculateInfoHash(infoDictionary)

            val trackers = ArrayList<String>()
            parsed.getByString("announce")?.let {
                trackers.add((it as BencodeValue.ByteString).toUTF8String())
            }
            parsed.getByString("announce-list")?.let {
                for (trackerList in (it as BencodeValue.List).value) {
                    for (tracker in (trackerList as BencodeValue.List).value) {
                        trackers.add((tracker as BencodeValue.ByteString).toUTF8String())
                    }
                }
            }

            val lengthEntry = infoDictionary.getByString("length") as BencodeValue.Integer?
            val filesEntry = infoDictionary.getByString("files") as BencodeValue.List?

            val (torrentSize: Long, fileStats: List<FileStats>) = when {
                lengthEntry !== null && filesEntry === null -> {
                    lengthEntry.value to listOf(FileStats(null, title, lengthEntry.value))
                }
                filesEntry !== null && lengthEntry === null -> {
                    var totalSizeAcc: Long = 0
                    val fileStats = filesEntry.value.mapIndexed { i, file ->
                        val fileAsDict = file as BencodeValue.Dictionary
                        val fileSize = (fileAsDict.getByString("length") as BencodeValue.Integer).value
                        val filePath = (fileAsDict.getByString("path") as BencodeValue.List).value.joinToString("/") {
                            (it as BencodeValue.ByteString).toUTF8String()
                        }

                        totalSizeAcc += fileSize
                        FileStats(
                            i + 1, // ids start at 1
                            filePath,
                            fileSize,
                        )
                    }
                    totalSizeAcc to fileStats
                }
                else -> throw RuntimeException("Invalid torrent file")
            }
            return Torrent(
                title,
                hash,
                torrentSize,
                trackers,
                fileStats,
            )
        } catch (e: ClassCastException) {
            throw RuntimeException("Invalid torrent file", e)
        }
    }

    private fun calculateInfoHash(infoDictionary: BencodeValue): String {
        // An output stream which discards all bytes, needed for use with DigestOutputStream
        class NullOutputStream : OutputStream() {
            override fun write(b: Int) {
                // discard bytes
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                // discard bytes
            }
        }

        val md = MessageDigest.getInstance("SHA-1")
        BencodeWriter.write(infoDictionary, DigestOutputStream(NullOutputStream(), md))
        val digestBytes = md.digest()

        val sb: StringBuilder = StringBuilder(digestBytes.size * 2)
        for (b in digestBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
