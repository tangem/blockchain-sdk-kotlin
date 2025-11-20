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
) : TransactionExtras