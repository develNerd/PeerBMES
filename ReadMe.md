# Binary Message Encoding Scheme (BMES) for WebRTC Signaling

This project implements a **Binary Message Encoding Scheme (BMES)** designed for WebRTC signaling. It encodes and decodes messages containing headers and a binary payload into a compact binary format. This README provides an overview of the project, its structure, and how to build and run it.

---


## Build and Run the Project

### Prerequisites

- **Java Development Kit (JDK)**: Ensure you have JDK 11 or later installed.
- **Gradle**: Ensure you have Gradle installed to build and run the project.

### Steps to Build and Run

1. **Clone the Repository**:
   ```bash
   git clone git@github.com:develNerd/PeerBMES.git
   cd bmes-webrtc
   ```

2. **Build the Project**:
   ```bash
   ./gradlew build
   ```

3. **Run the Tests**:
   ```bash
   ./gradlew test
   ```

4. **Run the Application**:
   ```bash
   ./gradlew run
   ```

---


#### Why Binary Encoding?
1. **Efficiency**:
    - Binary formats are more compact than text-based formats, reducing the size of the data transmitted over the network.
    - This is crucial for real-time communication systems like WebRTC, where low latency and high performance are critical.

2. **Speed**:
    - Binary data can be processed faster by machines because it avoids the overhead of parsing text-based formats.


## Structure of the Binary Message

A binary message in this scheme consists of the following components:
![Untitled-2024-12-15-1454xx](https://github.com/user-attachments/assets/b722aa68-2037-4e8d-9591-f36133ccae73)

---

## Binary Message Encoding Scheme Implementation

### Why These Sizes?

1. **Header Count (1 byte)**:
    - A single byte can represent values from `0` to `255`, which is more than enough for the maximum of 63 headers allowed in this scheme.

2. **Name Length and Value Length (2 bytes each)**:
    - Two bytes can represent values from `0` to `65535`, which is sufficient for the maximum header size of 1023 bytes.

3. **Payload Length (4 bytes)**:
    - Four bytes can represent values from `0` to `4,294,967,295`, which is more than enough for the maximum payload size of 256 KiB.


## Bit Storage Logic: Big Endian

### What is Big Endian?

- **Big Endian**: The most significant byte (MSB) is stored at the smallest memory address.
- **Little Endian**: The least significant byte (LSB) is stored at the smallest memory address.

### Why Big Endian?

1. **Network Standard**, **Readability**:
    - Big Endian is the standard for network protocols (e.g., TCP/IP). Using Big Endian ensures compatibility with other systems and protocols.


## Error Handling and Constraints
 
Examples include:

1. **Header Count Exceeds Maximum**:
    - Throws an `IllegalArgumentException` if the number of headers exceeds 63.

2. **Header Name/Value Exceeds Maximum Size**:
    - Throws an `IllegalArgumentException` if a header name or value exceeds 1023 bytes.

3. **Payload Exceeds Maximum Size**:
    - Throws an `IllegalArgumentException` if the payload exceeds 256 KiB.

4. **Malformed Binary Data**:
    - Throws an `IllegalArgumentException` if the binary data is incomplete or malformed.

### Constraints

1. **Maximum Headers**: 63.
2. **Maximum Header Name/Value Size**: 1023 bytes.
3. **Maximum Payload Size**: 256 KiB.
4. **ASCII Encoding**: Header names and values must be ASCII-encoded.

---

## Notes on Optimisation
- CPU and Memory Optimisation : 
    - There is a loop in the `encode` method that iterates over the headers to handle errors and constraints. 
      This can be optimised by checking the constraints along side asigning the values to the byte array.However, 
      the time complexity of that block of code is O(n) where n is the number of headers hence the worst case scenario is O(63).
      This is not a significant bottleneck as the number of headers is limited to 63.
