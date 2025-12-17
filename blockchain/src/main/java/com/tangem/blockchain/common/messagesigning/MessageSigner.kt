package com.tangem.blockchain.common.messagesigning

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result

/**
 * Provider for blockchain message signing operations.
 * Different blockchains implement different message signing standards.
 */
interface MessageSigner {
    /**
     * Signs a message with the specified address.
     *
     * @param message Message to sign (plain text)
     * @param address Address to sign with (must belong to wallet)
     * @param protocol Protocol-specific signing method (e.g., "ecdsa", "bip322")
     * @param signer Transaction signer (typically Tangem card)
     * @return Success with signature data, or Failure with error
     */
    suspend fun signMessage(
        message: String,
        address: String,
        protocol: String,
        signer: TransactionSigner,
    ): Result<MessageSignatureResult>
}

/**
 * Result of message signing operation.
 *
 * @property address Address used for signing
 * @property signature Signature as hex string
 * @property messageHash Optional message hash as hex string
 */
data class MessageSignatureResult(
    val address: String,
    val signature: String,
    val messageHash: String? = null,
)