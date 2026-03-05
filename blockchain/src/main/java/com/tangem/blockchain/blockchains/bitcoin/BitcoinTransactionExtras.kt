package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.common.TransactionExtras

/**
 * Additional parameters for Bitcoin transactions.
 *
 * @property memo Optional memo data to be included as OP_RETURN output (hex string without 0x prefix, max 80 bytes)
 * @property changeAddress Optional custom change address (must be one of wallet's addresses)
 */
data class BitcoinTransactionExtras(
    val memo: String? = null,
    val changeAddress: String? = null,
) : TransactionExtras {

    companion object {
        const val MAX_MEMO_SIZE_BYTES = 80

        /**
         * Creates BitcoinTransactionExtras with validated memo.
         */
        fun create(memo: String? = null, changeAddress: String? = null): BitcoinTransactionExtras {
            memo?.let { validateMemoLength(it) }
            return BitcoinTransactionExtras(memo, changeAddress)
        }

        /**
         * Validates memo hex string length.
         */
        private fun validateMemoLength(memoHex: String) {
            val cleanHex = memoHex.trim().removePrefix("0x").removePrefix("0X")
            require(cleanHex.length % 2 == 0) {
                "Memo hex string must have even length"
            }
            val byteLength = cleanHex.length / 2
            require(byteLength <= MAX_MEMO_SIZE_BYTES) {
                "Memo exceeds maximum size of $MAX_MEMO_SIZE_BYTES bytes (got $byteLength bytes)"
            }
        }
    }
}