package com.tangem.blockchain.blockchains.xrp

import com.tangem.blockchain.blockchains.xrp.network.XrpFeeResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpInfoResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.XrpTargetAccountResponse
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

class XrpNetworkProviderMock(
    private val isAccountCreatedResponse: Boolean = true,
) : XrpNetworkProvider {

    override val baseUrl: String
        get() = TODO("Not yet implemented")

    override suspend fun getInfo(address: String): Result<XrpInfoResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        TODO("Not yet implemented")
    }

    override suspend fun getFee(): Result<XrpFeeResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun checkIsAccountCreated(address: String) = isAccountCreatedResponse

    override suspend fun hasTransferRate(address: String): Boolean {
        return false
    }

    override suspend fun checkTargetAccount(address: String, token: Token?): Result<XrpTargetAccountResponse> {
        return Result.Success(XrpTargetAccountResponse(accountCreated = true, trustlineCreated = true))
    }

    override suspend fun getSequence(address: String): Result<Long> {
        return Result.Success(1406L)
    }

    override suspend fun checkDestinationTagRequired(address: String): Boolean {
        TODO("Not yet implemented")
    }
}