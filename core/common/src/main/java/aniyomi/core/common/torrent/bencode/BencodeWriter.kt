package aniyomi.core.common.torrent.bencode

import aniyomi.core.common.torrent.bencode.BencodeValue.ByteString
import java.io.OutputStream
import java.nio.CharBuffer

class BencodeWriter private constructor(val output: OutputStream) {
    companion object {
        fun write(v: BencodeValue, output: OutputStream) {
            val writer = BencodeWriter(output)
            writer.writeValue(v)
        }
    }

    private fun writeValue(v: BencodeValue) {
        when (v) {
            is BencodeValue.Integer -> writeInteger(v)
            is BencodeValue.ByteString -> writeByteString(v)
            is BencodeValue.List -> writeList(v)
            is BencodeValue.Dictionary -> writeDictionary(v)
        }
    }

    private fun writeInteger(v: BencodeValue.Integer) {
        output.write('i'.code)
        writeNumberHelper(v.value)
        output.write('e'.code)
    }

    private fun writeByteString(v: BencodeValue.ByteString) {
        writeNumberHelper(v.value.size.toLong())
        output.write(':'.code)
        output.write(v.value)
    }

    private fun writeList(v: BencodeValue.List) {
        output.write('l'.code)
        for (subval in v.value) {
            writeValue(subval)
        }
        output.write('e'.code)
    }

    private fun writeDictionary(v: BencodeValue.Dictionary) {
        output.write('d'.code)

        var prevKey: BencodeValue.ByteString? = null
        for (entry in v.value) {
            require(prevKey === null || entry.key > prevKey) { "Dictionary keys not in order" }
            prevKey = entry.key

            writeByteString(entry.key)
            writeValue(entry.value)
        }
        output.write('e'.code)
    }

    private fun writeNumberHelper(n: Long) {
        val encoder = Charsets.UTF_8.newEncoder()
        val str = n.toString(10)

        val buffer = encoder.encode(CharBuffer.wrap(str))
        val array = ByteArray(buffer.remaining())
        buffer.get(array)

        output.write(array)
    }
}
