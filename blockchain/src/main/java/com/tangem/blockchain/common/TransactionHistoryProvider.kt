package com.tangem.blockchain.common

import com.tangem.blockchain.extensions.Result

interface TransactionHistoryProvider {
    suspend fun getTransactionHistory(
        address: String,
        blockchain: Blockchain,
        tokens: Set<Token>,
    ): Result<List<TransactionData>>
}