class Message(
    val headers: MutableMap<String, String> = mutableMapOf(),
    var payload: ByteArray? = null
)

interface MessageCodec {
    fun encode(message: Message): ByteArray
    fun decode(data: ByteArray): Message
}

class PeerMessageCodec : MessageCodec {

    companion object {
        private const val MAX_HEADERS = 63
        private const val MAX_HEADER_SIZE = 1023
        private const val MAX_PAYLOAD_SIZE = 256 * 1024 // 256 KiB

        private const val HEADER_BYTE_SIZE = 2
    }

    override fun encode(message: Message): ByteArray {
        val asciiRegex = Regex("^[\\x00-\\x7F]*$") // Precompile the regex

        // Validate headers and calculate total size in one loop
        var totalSize = 1 // Header count (1 byte)
        val errors = StringBuilder()

        if (message.headers.size > MAX_HEADERS) {
            throw IllegalArgumentException("Too many headers. Maximum is $MAX_HEADERS.")
        }

        message.headers.forEach { (name, value) ->
            // Validate header name and value
            if (name.length > MAX_HEADER_SIZE || value.length > MAX_HEADER_SIZE) {
                errors.append("Header name or value exceeds $MAX_HEADER_SIZE bytes.\n")
            }
            if (!name.matches(asciiRegex) || !value.matches(asciiRegex)) {
                errors.append("Header name or value contains non-ASCII characters.\n")
            }

            // Calculate size for this header
            totalSize += (2 * HEADER_BYTE_SIZE) + name.length + value.length // Name length (2 bytes) + Value length (2 bytes) + name + value
        }

        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.toString())
        }

        // Validate payload
        if (message.payload != null && message.payload!!.size > MAX_PAYLOAD_SIZE) {
            throw IllegalArgumentException("Payload exceeds $MAX_PAYLOAD_SIZE bytes.")
        }

        // Add payload length and payload size to total size
        totalSize += 4 // Payload length (4 bytes)
        if (message.payload != null) {
            totalSize += message.payload!!.size
        }

        // Allocate buffer
        val data = ByteArray(totalSize)
        var offset = 0

        // Write header count
        data[offset++] = message.headers.size.toByte()

        // Write headers
        message.headers.forEach { (name, value) ->
            // Write name length (2 bytes)
            data[offset++] = (name.length shr 8).toByte()
            data[offset++] = name.length.toByte()

            // Write value length (2 bytes)
            data[offset++] = (value.length shr 8).toByte()
            data[offset++] = value.length.toByte()

            // Write name
            System.arraycopy(name.toByteArray(), 0, data, offset, name.length)
            offset += name.length

            // Write value
            System.arraycopy(value.toByteArray(), 0, data, offset, value.length)
            offset += value.length
        }

        // Write payload length (4 bytes)
        val payloadLength = message.payload?.size ?: 0
        data[offset++] = (payloadLength shr 24).toByte()
        data[offset++] = (payloadLength shr 16).toByte()
        data[offset++] = (payloadLength shr 8).toByte()
        data[offset++] = payloadLength.toByte()

        // Write payload
        if (message.payload != null) {
            System.arraycopy(message.payload, 0, data, offset, payloadLength)
        }

        return data
    }

    override fun decode(data: ByteArray): Message {
        if (data.isEmpty()) {
            throw IllegalArgumentException("Invalid data: empty byte array.")
        }

        val message = Message()
        var offset = 0

        // Read header count
        val headerCount = data[offset++].toInt() and 0xFF // 11111111 base 2

        // Read headers
        for (i in 0 until headerCount) {
            if (offset + 4 > data.size) {
                throw IllegalArgumentException("Invalid data: header length exceeds data size.")
            }

            // Read name length (2 bytes)
            val nameLength = ((data[offset++].toInt() and 0xFF) shl 8) or (data[offset++].toInt() and 0xFF)

            // Read value length (2 bytes)
            val valueLength = ((data[offset++].toInt() and 0xFF) shl 8) or (data[offset++].toInt() and 0xFF)

            // Read name
            if (offset + nameLength > data.size) {
                throw IllegalArgumentException("Invalid data: header name exceeds data size.")
            }
            val name = data.copyOfRange(offset, offset + nameLength).toString(Charsets.US_ASCII)
            offset += nameLength

            // Read value
            if (offset + valueLength > data.size) {
                throw IllegalArgumentException("Invalid data: header value exceeds data size.")
            }
            val value = data.copyOfRange(offset, offset + valueLength).toString(Charsets.US_ASCII)
            offset += valueLength

            message.headers[name] = value
        }

        // Read payload length (4 bytes)
        if (offset + 4 > data.size) {
            throw IllegalArgumentException("Invalid data: payload length exceeds data size.")
        }
        val payloadLength = ((data[offset++].toInt() and 0xFF) shl 24) or
                           ((data[offset++].toInt() and 0xFF) shl 16) or
                           ((data[offset++].toInt() and 0xFF) shl 8) or
                           (data[offset++].toInt() and 0xFF)

        // Read payload
        if (payloadLength > 0) {
            if (offset + payloadLength > data.size) {
                throw IllegalArgumentException("Invalid data: payload exceeds data size.")
            }
            message.payload = data.copyOfRange(offset, offset + payloadLength)
        }

        return message
    }
}