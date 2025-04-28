package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.BlockchainSdkError

object SolanaTransactionHelper {

    private const val SIGNATURE_LENGTH = 64

    /**
     * Removes signatures placeholders from transaction data
     * @param transaction: transaction data with placeholders
     * @returns Transaction data without placeholders
     */
    fun removeSignaturesPlaceholders(transaction: ByteArray): ByteArray {
        val firstByte = transaction.firstOrNull() ?: throw BlockchainSdkError.Solana.TransactionIsEmpty

        val signaturesPlaceholderLength = 1 + firstByte.toInt() * SIGNATURE_LENGTH

        return transaction.drop(signaturesPlaceholderLength).toByteArray()
    }
}