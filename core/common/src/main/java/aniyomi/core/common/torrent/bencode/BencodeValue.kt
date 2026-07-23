package aniyomi.core.common.torrent.bencode

import okio.ByteString.Companion.encode
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util.SortedMap

sealed interface BencodeValue {
    data class Integer(val value: Long) : BencodeValue

    data class ByteString(val value: ByteArray) : BencodeValue, Comparable<ByteString> {
        companion object {
            fun fromUTF8String(s: String): ByteString {
                val encoder = Charsets.UTF_8.newEncoder()

                val buffer = encoder.encode(CharBuffer.wrap(s))
                val array = ByteArray(buffer.remaining())
                buffer.get(array)

                return ByteString(array)
            }
        }

        fun toUTF8String(): String {
            val decoder = Charsets.UTF_8.newDecoder()
            return decoder.decode(ByteBuffer.wrap(this.value)).toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByteString

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }

        override fun compareTo(other: ByteString): Int {
            if ((this === other) || (this.value === other.value)) return 0

            this.value.zip(other.value).forEach { pair ->
                val comp = pair.first - pair.second
                if (comp != 0) return comp
            }

            return this.value.size - other.value.size
        }
    }

    data class List(val value: kotlin.collections.List<BencodeValue>) : BencodeValue

    data class Dictionary(val value: SortedMap<BencodeValue.ByteString, BencodeValue>) : BencodeValue {
        fun getByString(key: String): BencodeValue? {
            return this.value[ByteString.fromUTF8String(key)]
        }
    }
}
