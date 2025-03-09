import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PeerMessageCodecTest {

    private val codec = PeerMessageCodec()

    // Test valid messages with practical WebRTC headers
    @Test
    fun testEncodeDecode_WebRTCSignalingMessage() {
        val message = Message().apply {
            headers["sdp-type"] = "offer"
            headers["sdp-version"] = "1"
            payload = """
                {
                    "sdp": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n...",
                    "type": "offer"
                }
            """.trimIndent().toByteArray()
        }
        val encoded = codec.encode(message)
        val decoded = codec.decode(encoded)

        assertEquals(4, decoded.headers.size)
        assertEquals("offer", decoded.headers["sdp-type"])
        assertEquals("1", decoded.headers["sdp-version"])
        assertEquals(
            """
                {
                    "sdp": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n...",
                    "type": "offer"
                }
            """.trimIndent(),
            decoded.payload?.toString(Charsets.UTF_8)
        )
    }

    @Test
    fun testEncodeDecode_WebRTCICECandidateMessage() {
        val message = Message().apply {
            headers["sdp-type"] = "candidate"
            payload = """
                {
                    "candidate": "candidate:1 1 UDP 2122252543 192.168.1.1 12345 typ host",
                    "sdpMid": "0",
                    "sdpMLineIndex": 0
                }
            """.trimIndent().toByteArray()
        }
        val encoded = codec.encode(message)
        val decoded = codec.decode(encoded)

        assertEquals(3, decoded.headers.size)
        assertEquals("candidate", decoded.headers["sdp-type"])
        assertEquals(
            """
                {
                    "candidate": "candidate:1 1 UDP 2122252543 192.168.1.1 12345 typ host",
                    "sdpMid": "0",
                    "sdpMLineIndex": 0
                }
            """.trimIndent(),
            decoded.payload?.toString(Charsets.UTF_8)
        )
    }

    // Test invalid messages
    @Test
    fun testEncode_TooManyHeaders() {
        val message = Message().apply {
            repeat(64) { i ->
                headers["Header$i"] = "Value$i"
            }
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encode(message)
        }
    }

    @Test
    fun testEncode_HeaderNameTooLong() {
        val message = Message().apply {
            headers["A".repeat(1024)] = "Value"
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encode(message)
        }
    }

    @Test
    fun testEncode_HeaderValueTooLong() {
        val message = Message().apply {
            headers["Key"] = "V".repeat(1024)
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encode(message)
        }
    }

    @Test
    fun testEncode_NonAsciiHeaderName() {
        val message = Message().apply {
            headers["Header©"] = "Value"
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encode(message)
        }
    }

    @Test
    fun testEncode_NonAsciiHeaderValue() {
        val message = Message().apply {
            headers["Key"] = "Value©"
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encode(message)
        }
    }

    @Test
    fun testEncode_PayloadTooLarge() {
        val message = Message().apply {
            payload = ByteArray(256 * 1024 + 1) // 256 KiB + 1 byte
        }
        assertFailsWith<IllegalArgumentException> {
            codec.encode(message)
        }
    }

    // Test edge cases
    @Test
    fun testEncodeDecode_EmptyHeaderName() {
        val message = Message().apply {
            headers[""] = "Value"
        }
        val encoded = codec.encode(message)
        val decoded = codec.decode(encoded)

        assertEquals(1, decoded.headers.size)
        assertEquals("Value", decoded.headers[""])
    }

    @Test
    fun testEncodeDecode_EmptyHeaderValue() {
        val message = Message().apply {
            headers["Key"] = ""
        }
        val encoded = codec.encode(message)
        val decoded = codec.decode(encoded)

        assertEquals(1, decoded.headers.size)
        assertEquals("", decoded.headers["Key"])
    }

    @Test
    fun testEncodeDecode_NullPayload() {
        val message = Message().apply {
            headers["Key"] = "Value"
            payload = null
        }
        val encoded = codec.encode(message)
        val decoded = codec.decode(encoded)

        assertEquals(1, decoded.headers.size)
        assertEquals("Value", decoded.headers["Key"])
        assertNull(decoded.payload)
    }

    // Test malformed binary data (for decoding)
    @Test
    fun testDecode_InvalidHeaderCount() {
        val data = byteArrayOf(64) // Header count = 64 (exceeds max)
        assertFailsWith<IllegalArgumentException> {
            codec.decode(data)
        }
    }

    @Test
    fun testDecode_InvalidNameLength() {
        val data = byteArrayOf(
            1, // Header count = 1
            0x04, 0x00, // Name length = 1024 (exceeds max)
            0x00, 0x01, // Value length = 1
            // Missing name and value bytes
        )
        assertFailsWith<IllegalArgumentException> {
            codec.decode(data)
        }
    }

    @Test
    fun testDecode_InvalidPayloadLength() {
        val data = byteArrayOf(
            1, // Header count = 1
            0x00, 0x01, // Name length = 1
            0x00, 0x01, // Value length = 1
            'K'.code.toByte(), // Name = "K"
            'V'.code.toByte(), // Value = "V"
            0x00, 0x40, 0x00, 0x00 // Payload length = 4 MiB (exceeds max)
        )
        assertFailsWith<IllegalArgumentException> {
            codec.decode(data)
        }
    }

    @Test
    fun testDecode_EmptyData() {
        val data = byteArrayOf()
        assertFailsWith<IllegalArgumentException> {
            codec.decode(data)
        }
    }

    // Round-trip tests
    @Test
    fun testRoundTrip_WebRTCSignalingMessage() {
        val message = Message().apply {
            headers["sdp-type"] = "offer"
            headers["sdp-version"] = "1"
            headers["ice-ufrag"] = "uFrag123"
            headers["ice-pwd"] = "pwd456"
            payload = """
                {
                    "sdp": "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n...",
                    "type": "offer"
                }
            """.trimIndent().toByteArray()
        }
        val encoded = codec.encode(message)
        val decoded = codec.decode(encoded)

        assertEquals(message.headers, decoded.headers)
        assertEquals(message.payload?.toString(Charsets.UTF_8), decoded.payload?.toString(Charsets.UTF_8))
    }
}