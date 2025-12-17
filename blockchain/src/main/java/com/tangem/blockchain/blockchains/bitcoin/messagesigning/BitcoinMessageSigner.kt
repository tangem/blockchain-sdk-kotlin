package com.tangem.blockchain.blockchains.bitcoin.messagesigning

import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageProtocol
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.messagesigning.MessageSignatureResult
import com.tangem.blockchain.common.messagesigning.MessageSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.model.PublicKey
import org.spongycastle.crypto.digests.SHA256Digest
import java.math.BigInteger
import java.nio.charset.StandardCharsets

/**
 * Bitcoin implementation of message signing.
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
) : MessageSigner {

    override suspend fun signMessage(
        message: String,
        address: String,
        protocol: String,
        signer: TransactionSigner,
    ): Result<MessageSignatureResult> {
        val walletAddress = validateAddress(address).successOr { return it }
        val signProtocol = SignMessageProtocol.fromString(protocol)
        validateProtocol(signProtocol).successOr { return it }

        val messageHash = createBitcoinMessageHash(message)
        val signature = signHash(messageHash, signer).successOr { return it }
        validateSignatureSize(signature).successOr { return it }

        val bitcoinSignature = createBitcoinSignature(signature, walletAddress.type, messageHash)

        return Result.Success(
            MessageSignatureResult(
                address = address,
                signature = bitcoinSignature.toHexString(),
                messageHash = messageHash.toHexString(),
            ),
        )
    }

    /**
     * Validates that address belongs to wallet.
     */
    private fun validateAddress(address: String): Result<com.tangem.blockchain.common.address.Address> {
        return wallet.addresses.find { it.value == address }
            ?.let { Result.Success(it) }
            ?: Result.Failure(
                BlockchainSdkError.CustomError("Address $address does not belong to this wallet"),
            )
    }

    /**
     * Validates signing protocol.
     */
    private fun validateProtocol(protocol: SignMessageProtocol): Result<Unit> {
        return when (protocol) {
            SignMessageProtocol.BIP322 -> Result.Failure(
                BlockchainSdkError.CustomError("BIP322 message signing is not yet supported. Use ECDSA protocol."),
            )
            else -> Result.Success(Unit)
        }
    }

    /**
     * Signs message hash using signer and extracts the signature.
     */
    private suspend fun signHash(messageHash: ByteArray, signer: TransactionSigner): Result<ByteArray> {
        return when (val result = signer.sign(listOf(messageHash), wallet.publicKey)) {
            is CompletionResult.Success -> result.data.firstOrNull()
                ?.let { Result.Success(it) }
                ?: Result.Failure(BlockchainSdkError.CustomError("Signer returned empty signature list"))
            is CompletionResult.Failure -> Result.fromTangemSdkError(result.error)
        }
    }

    /**
     * Validates signature size.
     */
    private fun validateSignatureSize(signature: ByteArray): Result<Unit> {
        return if (signature.size == SIGNATURE_SIZE) {
            Result.Success(Unit)
        } else {
            Result.Failure(
                BlockchainSdkError.CustomError(
                    "Invalid signature size: expected $SIGNATURE_SIZE bytes, got ${signature.size}",
                ),
            )
        }
    }

    /**
     * Creates Bitcoin signature format with recovery header.
     *
     * Computes the correct recovery ID (0-3) by attempting to recover the public key
     * from the signature and verifying it matches the wallet's public key.
     */
    private fun createBitcoinSignature(
        signature: ByteArray,
        addressType: AddressType,
        messageHash: ByteArray,
    ): ByteArray {
        // Parse signature into r and s components
        val r = BigInteger(1, signature.copyOfRange(0, SIGNATURE_COMPONENT_SIZE))
        val s = BigInteger(1, signature.copyOfRange(SIGNATURE_COMPONENT_SIZE, SIGNATURE_SIZE))
        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        // Determine recovery ID by attempting to recover the public key
        val publicKeyBytes = wallet.publicKey.blockchainKey.toDecompressedPublicKey()
        val recId = ecdsaSignature.determineRecId(
            messageHash = messageHash,
            publicKey = PublicKey(publicKeyBytes.sliceArray(1..publicKeyBytes.lastIndex)),
        )

        // Get base header byte for address type and add recovery ID
        val baseHeaderByte = getBaseRecoveryHeaderByte(addressType)
        val headerByte = (baseHeaderByte + recId).toByte()

        return byteArrayOf(headerByte) + signature
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
        val prefix = MESSAGE_PREFIX.toByteArray(StandardCharsets.UTF_8)

        val data = byteArrayOf(prefix.size.toByte()) +
            prefix +
            encodeVarint(messageBytes.size) +
            messageBytes

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
            value < VARINT_SINGLE_BYTE_LIMIT -> encodeSingleByteVarint(value)
            value <= VARINT_TWO_BYTE_LIMIT -> encodeTwoByteVarint(value)
            else -> encodeFourByteVarint(value)
        }
    }

    private fun encodeSingleByteVarint(value: Int): ByteArray = byteArrayOf(value.toByte())

    private fun encodeTwoByteVarint(value: Int): ByteArray = byteArrayOf(
        0xfd.toByte(),
        value.toByte(),
        (value shr BYTE_SHIFT).toByte(),
    )

    private fun encodeFourByteVarint(value: Int): ByteArray = byteArrayOf(
        0xfe.toByte(),
        value.toByte(),
        (value shr BYTE_SHIFT).toByte(),
        (value shr TWO_BYTES_SHIFT).toByte(),
        (value shr THREE_BYTES_SHIFT).toByte(),
    )

    /**
     * Gets the base recovery header byte based on address type.
     *
     * Header byte values:
     * - 27-30: P2PKH uncompressed
     * - 31-34: P2PKH compressed (Legacy)
     * - 35-38: P2WPKH-P2SH (SegWit)
     * - 39-42: P2WPKH (Native SegWit)
     *
     * The actual recovery ID (0-3) is added to this base value to create the final header byte.
     *
     * @param addressType Address type
     * @return Header byte base value (without recovery ID)
     */
    private fun getBaseRecoveryHeaderByte(addressType: AddressType): Int {
        return when (addressType) {
            AddressType.Legacy -> HEADER_P2PKH_COMPRESSED.toInt()
            AddressType.Default -> HEADER_P2WPKH_NATIVE.toInt()
            else -> HEADER_P2PKH_COMPRESSED.toInt()
        }
    }

    private companion object {
        const val SIGNATURE_SIZE = 64
        const val SIGNATURE_COMPONENT_SIZE = 32
        const val MESSAGE_PREFIX = "Bitcoin Signed Message:\n"
        const val VARINT_SINGLE_BYTE_LIMIT = 253
        const val VARINT_TWO_BYTE_LIMIT = 0xffff
        const val HEADER_P2PKH_COMPRESSED: Byte = 31
        const val HEADER_P2WPKH_NATIVE: Byte = 39
        const val BYTE_SHIFT = 8
        const val TWO_BYTES_SHIFT = 16
        const val THREE_BYTES_SHIFT = 24
    }
}