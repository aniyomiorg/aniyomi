package aniyomi.core.common.torrent

import aniyomi.core.common.torrent.model.FileStats
import aniyomi.core.common.torrent.model.Torrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.InputStream

class TorrentHelpersTest {
    @TestFactory
    fun test_parseTorrentDetailsFromTorrentFileContent(): Collection<DynamicTest> {
        data class TestCase(val inputFile: String, val expected: Torrent)

        val testBody = { t: TestCase ->
            val inputStream = getTestResource(t.inputFile)
            val actual = TorrentHelpers.parseTorrentDetailsFromTorrentFileContent(inputStream)
            assertEquals(t.expected, actual)
        }

        val testCases = listOf<TestCase>(
            TestCase(
                "test-torrent-file.torrent",
                Torrent(
                    "test-torrent-file",
                    "104701de4b710ee9720ee249bf220384659e8c38",
                    18,
                    listOf<String>(
                        "udp://tracker.opentrackr.org:1337/announce",
                    ),
                    listOf<FileStats>(
                        FileStats(null, "test-torrent-file", 18),
                    ),
                ),
            ),
            TestCase(
                "test-torrent-directory.torrent",
                Torrent(
                    "test-torrent-directory",
                    "863eb76f1cd0643d9fd2eb4a7da866c3752991b5",
                    12,
                    listOf<String>(
                        "udp://tracker.opentrackr.org:1337/announce",
                        "udp://tracker.opentrackr.org:1337/announce",
                        "https://tracker.tamersunion.org:443/announce",
                        "http://tracker.ipv6tracker.org:80/announce",
                        "udp://opentracker.io:6969/announce",
                        "udp://tracker.torrent.eu.org:451/announce",
                    ),
                    listOf<FileStats>(
                        FileStats(1, "3/3.1", 4),
                        FileStats(2, "2", 2),
                        FileStats(3, "1", 2),
                        FileStats(4, "3/3.2", 4),
                    ),
                ),
            ),
        )

        return testCases.map {
            dynamicTest(it.inputFile) { testBody(it) }!!
        }
    }
    private fun getTestResource(relativeResourcePath: String): InputStream {
        return this.javaClass.classLoader!!.getResourceAsStream("aniyomi/core/common/torrent/$relativeResourcePath")!!
    }
}
