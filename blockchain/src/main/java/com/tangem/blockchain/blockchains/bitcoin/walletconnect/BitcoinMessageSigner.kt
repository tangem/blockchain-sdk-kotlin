package com.tangem.blockchain.blockchains.bitcoin.walletconnect

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageProtocol
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import org.spongycastle.crypto.digests.SHA256Digest
import java.nio.charset.StandardCharsets

/**
 * Handler for Bitcoin message signing operations.
 *
 * Implements Bitcoin message signing according to the standard format:
 * - ECDSA: Standard Bitcoin message signing (BIP137 compatible)
 * - BIP322: Generic signed message format (future support)
 *
 * @property wallet The Bitcoin wallet instance
 *
 * @see <a href="https://en.bitcoin.it/wiki/Message_signing">Bitcoin Message Signing</a>
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0137.mediawiki">BIP-137</a>
 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0322.mediawiki">BIP-322</a>
 */
internal class BitcoinMessageSigner(
    private val wallet: Wallet,
) {

    /**
     * Signs a message using Bitcoin message signing format.
     *
     * This method:
     * 1. Validates that the signing address belongs to this wallet
     * 2. Creates the Bitcoin Signed Message format: "\x18Bitcoin Signed Message:\n" + message
     * 3. Computes double SHA256 hash
     * 4. Signs the hash using the provided signer
     * 5. Returns signature in Bitcoin message signature format (65 bytes: header + r + s)
     *
     * @param message Message to sign
     * @param address Address to sign with
     * @param protocol Signing protocol (ECDSA or BIP322)
     * @param signer Transaction signer (typically Tangem card)
     * @return Success with signature data, or Failure with error
     */
    suspend fun signMessage(
        message: String,
        address: String,
        protocol: SignMessageProtocol,
        signer: TransactionSigner,
    ): Result<SignMessageResult> {
        // Validate that the address belongs to this wallet
        val walletAddress = wallet.addresses.find { it.value == address }
            ?: return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Address $address does not belong to this wallet",
                ),
            )

        // Currently only ECDSA is supported
        if (protocol == SignMessageProtocol.BIP322) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "BIP322 message signing is not yet supported. Use ECDSA protocol.",
                ),
            )
        }

        // Create Bitcoin Signed Message format
        val messageHash = createBitcoinMessageHash(message)

        // Sign the hash
        val signatureResult = signer.sign(listOf(messageHash), wallet.publicKey)
        val signature = when (signatureResult) {
            is CompletionResult.Success -> signatureResult.data.firstOrNull()
                ?: return Result.Failure(
                    BlockchainSdkError.CustomError("Signer returned empty signature list"),
                )
            is CompletionResult.Failure -> return Result.fromTangemSdkError(signatureResult.error)
        }

        // Signature from Tangem is 64 bytes (32 r + 32 s)
        if (signature.size != 64) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Invalid signature size: expected 64 bytes, got ${signature.size}",
                ),
            )
        }

        // Create Bitcoin message signature format (65 bytes: header + r + s)
        val headerByte = getRecoveryHeaderByte(walletAddress.type)
        val bitcoinSignature = byteArrayOf(headerByte).plus(signature)

        return Result.Success(
            SignMessageResult(
                address = address,
                signature = bitcoinSignature.toHexString(),
                messageHash = messageHash.toHexString(),
            ),
        )
    }

    /**
     * Creates Bitcoin message hash according to standard format.
     *
     * Format: double SHA256 of: "\x18Bitcoin Signed Message:\n" + varint(len(message)) + message
     *
     * @param message Original message
     * @return 32-byte hash to sign
     */
    private fun createBitcoinMessageHash(message: String): ByteArray {
        val messageBytes = message.toByteArray(StandardCharsets.UTF_8)
        val messageLength = messageBytes.size

        // Bitcoin Signed Message prefix
        val prefix = "Bitcoin Signed Message:\n".toByteArray(StandardCharsets.UTF_8)
        val prefixLength = byteArrayOf(prefix.size.toByte())

        // Message length as varint (for simplicity, assume message < 253 bytes for single byte varint)
        val messageLengthVarint = if (messageLength < 253) {
            byteArrayOf(messageLength.toByte())
        } else {
            // For longer messages, use varint encoding
            encodeVarint(messageLength)
        }

        // Combine: prefixLength + prefix + messageLengthVarint + message
        val data = prefixLength + prefix + messageLengthVarint + messageBytes

        // Double SHA256
        return doubleSha256(data)
    }

    /**
     * Computes double SHA256 hash.
     */
    private fun doubleSha256(data: ByteArray): ByteArray {
        val digest = SHA256Digest()
        val firstHash = ByteArray(digest.digestSize)

        digest.update(data, 0, data.size)
        digest.doFinal(firstHash, 0)

        digest.reset()
        val secondHash = ByteArray(digest.digestSize)
        digest.update(firstHash, 0, firstHash.size)
        digest.doFinal(secondHash, 0)

        return secondHash
    }

    /**
     * Encodes integer as Bitcoin varint.
     */
    private fun encodeVarint(value: Int): ByteArray {
        return when {
            value < 0xfd -> byteArrayOf(value.toByte())
            value <= 0xffff -> byteArrayOf(
                0xfd.toByte(),
                (value and 0xff).toByte(),
                ((value shr 8) and 0xff).toByte(),
            )
            else -> byteArrayOf(
                0xfe.toByte(),
                (value and 0xff).toByte(),
                ((value shr 8) and 0xff).toByte(),
                ((value shr 16) and 0xff).toByte(),
                ((value shr 24) and 0xff).toByte(),
            )
        }
    }

    /**
     * Gets the recovery header byte based on address type.
     *
     * Header byte values:
     * - 27-30: P2PKH uncompressed
     * - 31-34: P2PKH compressed (Legacy)
     * - 35-38: P2WPKH-P2SH (SegWit)
     * - 39-42: P2WPKH (Native SegWit)
     *
     * Note: The +0 to +3 offset is determined during signature recovery.
     * We use base values here, actual recovery ID will be added during verification.
     *
     * @param addressType Address type
     * @return Header byte base value
     */
    private fun getRecoveryHeaderByte(addressType: AddressType): Byte {
        return when (addressType) {
            AddressType.Legacy -> 31.toByte() // P2PKH compressed
            AddressType.Default -> 39.toByte() // P2WPKH native segwit (bc1q...)
            else -> 31.toByte() // Default to compressed P2PKH
        }
    }
}

/**
 * Result of message signing operation.
 *
 * @property address Address used for signing
 * @property signature Signature as hex string (65 bytes: header + r + s)
 * @property messageHash Message hash as hex string (32 bytes)
 */
data class SignMessageResult(
    val address: String,
    val signature: String,
    val messageHash: String,
)