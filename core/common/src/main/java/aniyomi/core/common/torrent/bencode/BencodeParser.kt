package aniyomi.core.common.torrent.bencode

import java.io.EOFException
import java.io.InputStream
import java.util.TreeMap

class BencodeParser private constructor(val input: InputStream) {
    companion object {
        fun parse(input: InputStream): BencodeValue {
            val parser = BencodeParser(input)
            val result = parser.parseValue(parser.readNextByte())
            require(input.read() == -1) { "Unexpected extra data" }
            return result
        }

        private const val BYTE_CHAR_LOWERCASE_I: Byte = 'i'.code.toByte()
        private const val BYTE_CHAR_LOWERCASE_L: Byte = 'l'.code.toByte()
        private const val BYTE_CHAR_LOWERCASE_D: Byte = 'd'.code.toByte()
        private const val BYTE_CHAR_LOWERCASE_E: Byte = 'e'.code.toByte()
        private const val BYTE_CHAR_COLON: Byte = ':'.code.toByte()
        private const val BYTE_CHAR_HYPHEN: Byte = '-'.code.toByte()
        private const val BYTE_CHAR_0: Byte = '0'.code.toByte()
        private const val BYTE_CHAR_1: Byte = '1'.code.toByte()
        private const val BYTE_CHAR_9: Byte = '9'.code.toByte()
    }

    private fun parseValue(head: Byte): BencodeValue {
        return when (head) {
            BYTE_CHAR_LOWERCASE_I -> parseInteger()
            in BYTE_CHAR_0..BYTE_CHAR_9 -> parseByteString(head)
            BYTE_CHAR_LOWERCASE_L -> parseList()
            BYTE_CHAR_LOWERCASE_D -> parseDictionary()
            else -> throw IllegalArgumentException("Unexpected value type")
        }
    }

    private fun parseInteger(): BencodeValue.Integer {
        val result = parseNumberHelper(readNextByte(), BYTE_CHAR_LOWERCASE_E)
        return BencodeValue.Integer(result)
    }

    private fun parseByteString(head: Byte): BencodeValue.ByteString {
        val length = parseStringLength(head)
        val result = ByteArray(length)
        for (i in 0..<length) {
            result[i] = readNextByte()
        }
        return BencodeValue.ByteString(result)
    }

    private fun parseStringLength(head: Byte): Int {
        val result = parseNumberHelper(head, BYTE_CHAR_COLON)
        require(result in 0..Int.MAX_VALUE) { "Invalid string length" }
        return result.toInt()
    }

    private fun parseList(): BencodeValue.List {
        val result = ArrayList<BencodeValue>()
        while (true) {
            val b = readNextByte()
            if (b == BYTE_CHAR_LOWERCASE_E) {
                break
            }
            result.add(parseValue(b))
        }
        return BencodeValue.List(result)
    }

    private fun parseDictionary(): BencodeValue.Dictionary {
        val result = TreeMap<BencodeValue.ByteString, BencodeValue>()
        while (true) {
            val b = readNextByte()
            if (b == BYTE_CHAR_LOWERCASE_E) {
                break
            }
            val key = parseByteString(b)
            require(result.isEmpty() || result.lastKey() < key) { "Dictionary keys out of order" }
            result[key] = parseValue(readNextByte())
        }
        return BencodeValue.Dictionary(result)
    }

    private fun parseNumberHelper(head: Byte, terminatingCharacter: Byte): Long {
        val sb = StringBuilder()

        var b = head
        do {
            val ok: Boolean = when {
                // First character can be minus sign or digit
                sb.isEmpty() -> b == BYTE_CHAR_HYPHEN || (b in BYTE_CHAR_0..BYTE_CHAR_9)
                // Can't have any other characters if the number is zero
                sb.contentEquals("0") -> false
                // Can't have minus zero, so after hyphen only 1-9 are valid
                sb.contentEquals("-") -> (b in BYTE_CHAR_1..BYTE_CHAR_9)
                // Remaining characters must be digits
                else -> (b in BYTE_CHAR_0..BYTE_CHAR_9)
            }
            require(ok) { "Unexpected integer character" }
            sb.append(Char(b.toUShort()))
        } while ((readNextByte().also { b = it }) != terminatingCharacter)

        return sb.toString().toLong()
    }

    private fun readNextByte(): Byte {
        val result = input.read()
        if (result == -1) {
            throw EOFException()
        }
        return result.toByte()
    }
}
