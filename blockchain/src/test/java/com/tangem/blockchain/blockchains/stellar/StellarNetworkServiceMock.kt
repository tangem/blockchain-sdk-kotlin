package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

class StellarNetworkServiceMock(
        private val isAccountCreatedResponse: Boolean = true
) : StellarNetworkProvider {
    override suspend fun getInfo(accountId: String): Result<StellarResponse> {
        TODO("Not yet implemented")
    }
    override suspend fun sendTransaction(transaction: String): SimpleResult {
        TODO("Not yet implemented")
    }
    override suspend fun checkIsAccountCreated(address: String) = isAccountCreatedResponse
    override suspend fun getSignatureCount(accountId: String): Result<Int> {
        TODO("Not yet implemented")
    }
}