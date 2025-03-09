fun main() {
    val codec = PeerMessageCodec()

    // Create a message
    val message = Message().apply {
        headers["Content-Type"] = "application/json"
        headers["Authorization"] = "Bearer token"
        payload = "Hello, World!".toByteArray()
    }

    // Encode the message
    val encodedMessage = codec.encode(message)
    println("Encoded message: ${encodedMessage.joinToString(" ") { "%02x".format(it) }}")

    // Decode the message
    val decodedMessage = codec.decode(encodedMessage)
    println("Decoded message headers: ${decodedMessage.headers}")
    println("Decoded message payload: ${decodedMessage.payload?.toString(Charsets.UTF_8)}")
}