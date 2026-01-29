package com.tangem.blockchain.common

import com.tangem.common.CompletionResult

interface MessageSigner {
    suspend fun signMessage(message: String, signer: TransactionSigner): CompletionResult<String>
}