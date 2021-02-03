package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

interface StellarNetworkService {
    suspend fun getInfo(accountId: String): Result<StellarResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun checkIsAccountCreated(address: String): Boolean
    suspend fun getSignatureCount(accountId: String): Result<Int>
}