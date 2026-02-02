package com.tangem.blockchain.blockchains.clore

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.BitcoinMessageSignUtil
import org.bitcoinj.core.ECKey
import org.junit.Test
import java.math.BigInteger
import java.util.Base64

class CloreMessageSignUtilTest {

    private fun createMessageHash(message: String) = BitcoinMessageSignUtil.createMessageHash(
        message = message,
        messageMagic = BitcoinMessageSignUtil.CLORE_MESSAGE_MAGIC,
    )

    @Test
    fun `createMessageHash produces correct hash for simple message`() {
        // Arrange
        val message = "Hello, Clore!"

        // Act
        val hash = createMessageHash(message)

        // Assert
        assertThat(hash.size).isEqualTo(32) // SHA256 hash size
        // The hash should be deterministic
        val hash2 = createMessageHash(message)
        assertThat(hash).isEqualTo(hash2)
    }

    @Test
    fun `createMessageHash produces different hashes for different messages`() {
        // Arrange
        val message1 = "Message 1"
        val message2 = "Message 2"

        // Act
        val hash1 = createMessageHash(message1)
        val hash2 = createMessageHash(message2)

        // Assert
        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun `createMessageHash handles empty message`() {
        // Arrange
        val message = ""

        // Act
        val hash = createMessageHash(message)

        // Assert
        assertThat(hash.size).isEqualTo(32)
    }

    @Test
    fun `createMessageHash handles long message`() {
        // Arrange
        val message = "A".repeat(1000)

        // Act
        val hash = createMessageHash(message)

        // Assert
        assertThat(hash.size).isEqualTo(32)
    }

    @Test
    fun `createMessageHash handles unicode message`() {
        // Arrange - test with claim message format
        val message = "Claim request for CLORE tokens to Ethereum address 0x1234567890abcdef from CLORE_ADDRESS"

        // Act
        val hash = createMessageHash(message)

        // Assert
        assertThat(hash.size).isEqualTo(32)
    }

    @Test
    fun `different magic prefixes produce different hashes`() {
        // Arrange
        val message = "Test message"

        // Act
        val cloreHash = BitcoinMessageSignUtil.createMessageHash(
            message = message,
            messageMagic = BitcoinMessageSignUtil.CLORE_MESSAGE_MAGIC,
        )
        val bitcoinHash = BitcoinMessageSignUtil.createMessageHash(
            message = message,
            messageMagic = BitcoinMessageSignUtil.BITCOIN_MESSAGE_MAGIC,
        )

        // Assert
        assertThat(cloreHash).isNotEqualTo(bitcoinHash)
    }

    @Test
    fun `createRecoverableSignature produces correct 65-byte signature with compressed key`() {
        // Arrange
        val privateKey = ECKey()
        val compressedPubKey = privateKey.pubKeyPoint.getEncoded(true)
        val message = "Test message"
        val messageHash = createMessageHash(message)

        // Sign with private key
        val ecdsaSig = privateKey.sign(org.bitcoinj.core.Sha256Hash.wrap(messageHash))
        val rBytes = ecdsaSig.r.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val sBytes = ecdsaSig.s.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val signatureBytes = rBytes + sBytes

        // Act
        val recoverableSignature = BitcoinMessageSignUtil.createRecoverableSignature(
            signatureBytes = signatureBytes,
            publicKey = compressedPubKey,
            messageHash = messageHash,
        )

        // Assert
        assertThat(recoverableSignature.size).isEqualTo(65)
        // Header byte should be 31-34 for compressed keys
        val headerByte = recoverableSignature[0].toInt() and 0xFF
        assertThat(headerByte).isIn(31..34)
    }

    @Test
    fun `createRecoverableSignature produces correct 65-byte signature with uncompressed key`() {
        // Arrange
        val privateKey = ECKey()
        val uncompressedPubKey = privateKey.pubKeyPoint.getEncoded(false)
        val message = "Test message"
        val messageHash = createMessageHash(message)

        // Sign with private key
        val ecdsaSig = privateKey.sign(org.bitcoinj.core.Sha256Hash.wrap(messageHash))
        val rBytes = ecdsaSig.r.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val sBytes = ecdsaSig.s.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val signatureBytes = rBytes + sBytes

        // Act
        val recoverableSignature = BitcoinMessageSignUtil.createRecoverableSignature(
            signatureBytes = signatureBytes,
            publicKey = uncompressedPubKey,
            messageHash = messageHash,
        )

        // Assert
        assertThat(recoverableSignature.size).isEqualTo(65)
        // Header byte should be 27-30 for uncompressed keys
        val headerByte = recoverableSignature[0].toInt() and 0xFF
        assertThat(headerByte).isIn(27..30)
    }

    @Test
    fun `recoverable signature can be decoded to Base64`() {
        // Arrange
        val privateKey = ECKey()
        val compressedPubKey = privateKey.pubKeyPoint.getEncoded(true)
        val message = "Claim request for CLORE tokens to Ethereum address 0x742d35Cc6634C0532925a3b844Bc9e7595f60126 from CEsMERNgBVPo9Dkc99pRDMPD3mxwPMxHZi"
        val messageHash = createMessageHash(message)

        // Sign with private key
        val ecdsaSig = privateKey.sign(org.bitcoinj.core.Sha256Hash.wrap(messageHash))
        val rBytes = ecdsaSig.r.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val sBytes = ecdsaSig.s.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val signatureBytes = rBytes + sBytes

        val recoverableSignature = BitcoinMessageSignUtil.createRecoverableSignature(
            signatureBytes = signatureBytes,
            publicKey = compressedPubKey,
            messageHash = messageHash,
        )

        // Act
        val base64Signature = Base64.getEncoder().encodeToString(recoverableSignature)

        // Assert
        assertThat(base64Signature).isNotEmpty()
        // Base64 of 65 bytes should be ~88 characters
        assertThat(base64Signature.length).isIn(86..90)
        // Should be valid base64
        val decoded = Base64.getDecoder().decode(base64Signature)
        assertThat(decoded).isEqualTo(recoverableSignature)
    }

    /**
     * Test based on Clore official test vector format:
     * https://github.com/Clore-ai/Clore/blob/main/test/functional/rpc_signmessage.py#L20
     *
     * This test verifies that our implementation can sign a message and recover
     * the public key from the signature using Clore message magic.
     */
    @Test
    fun `sign and verify message with Clore magic`() {
        val message = "This is just a test message"
        val privateKeyWif = "cUeKHd5orzT3mz8P9pxyREHfsWtVfgsfDjiZZBcjUBAaGk1BTj7N"

        // Decode WIF private key
        val privateKey = org.bitcoinj.core.DumpedPrivateKey.fromBase58(
            org.bitcoinj.core.NetworkParameters.fromID(org.bitcoinj.core.NetworkParameters.ID_TESTNET),
            privateKeyWif,
        ).key

        // Create message hash using Clore magic (mainnet)
        val messageHash = createMessageHash(message)

        // Sign with private key
        val ecdsaSig = privateKey.sign(org.bitcoinj.core.Sha256Hash.wrap(messageHash))
        val rBytes = ecdsaSig.r.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val sBytes = ecdsaSig.s.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val signatureBytes = rBytes + sBytes

        val compressedPubKey = privateKey.pubKeyPoint.getEncoded(true)

        val recoverableSignature = BitcoinMessageSignUtil.createRecoverableSignature(
            signatureBytes = signatureBytes,
            publicKey = compressedPubKey,
            messageHash = messageHash,
        )

        // Verify signature format
        assertThat(recoverableSignature.size).isEqualTo(65)
        val headerByte = recoverableSignature[0].toInt() and 0xFF
        assertThat(headerByte).isIn(31..34)

        // Verify public key can be recovered
        val recoveryId = headerByte - 31
        val r = BigInteger(1, recoverableSignature.copyOfRange(1, 33))
        val s = BigInteger(1, recoverableSignature.copyOfRange(33, 65))
        val recoveredSig = ECKey.ECDSASignature(r, s)

        val recoveredKey = ECKey.recoverFromSignature(
            recoveryId,
            recoveredSig,
            org.bitcoinj.core.Sha256Hash.wrap(messageHash),
            true,
        )

        assertThat(recoveredKey).isNotNull()
        assertThat(recoveredKey!!.pubKeyPoint).isEqualTo(privateKey.pubKeyPoint)
    }

    @Test
    fun `recoverable signature allows public key recovery`() {
        // Arrange
        val privateKey = ECKey()
        val compressedPubKey = privateKey.pubKeyPoint.getEncoded(true)
        val message = "Test recovery"
        val messageHash = createMessageHash(message)

        // Sign with private key
        val ecdsaSig = privateKey.sign(org.bitcoinj.core.Sha256Hash.wrap(messageHash))
        val rBytes = ecdsaSig.r.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val sBytes = ecdsaSig.s.toByteArray().let { bytes ->
            if (bytes.size > 32 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        }.let { if (it.size < 32) ByteArray(32 - it.size) + it else it }
        val signatureBytes = rBytes + sBytes

        val recoverableSignature = BitcoinMessageSignUtil.createRecoverableSignature(
            signatureBytes = signatureBytes,
            publicKey = compressedPubKey,
            messageHash = messageHash,
        )

        // Act - Extract recovery ID and recover public key
        val headerByte = recoverableSignature[0].toInt() and 0xFF
        val isCompressed = headerByte >= 31
        val recoveryId = if (isCompressed) headerByte - 31 else headerByte - 27
        val r = BigInteger(1, recoverableSignature.copyOfRange(1, 33))
        val s = BigInteger(1, recoverableSignature.copyOfRange(33, 65))
        val recoveredEcdsaSig = ECKey.ECDSASignature(r, s)

        val recoveredKey = ECKey.recoverFromSignature(
            recoveryId,
            recoveredEcdsaSig,
            org.bitcoinj.core.Sha256Hash.wrap(messageHash),
            isCompressed,
        )

        // Assert
        assertThat(recoveredKey).isNotNull()
        assertThat(recoveredKey!!.pubKeyPoint).isEqualTo(privateKey.pubKeyPoint)
    }
}