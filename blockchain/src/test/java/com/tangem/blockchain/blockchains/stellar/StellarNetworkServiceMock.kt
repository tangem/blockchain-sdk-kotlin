package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import org.stellar.sdk.responses.FeeStatsResponse

class StellarNetworkServiceMock(
    private val checkTargetAccountResult: Result<StellarTargetAccountResponse> = Result.Success(
        StellarTargetAccountResponse(accountCreated = true, trustlineCreated = true),
    ),
) : StellarNetworkProvider {

    override val baseUrl: String
        get() = TODO("Not yet implemented")

    override suspend fun getInfo(accountId: String): Result<StellarResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getFeeStats(): Result<FeeStatsResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        TODO("Not yet implemented")
    }

    override suspend fun checkTargetAccount(address: String, token: Token?): Result<StellarTargetAccountResponse> =
        checkTargetAccountResult

    override suspend fun getSignatureCount(accountId: String): Result<Int> {
        TODO("Not yet implemented")
    }
}
