package com.tangem.blockchain.blockchains.icp.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import okhttp3.RequestBody.Companion.toRequestBody
import org.ic4j.agent.icp.IcpSystemCanisters

internal class InternetComputerNetworkProvider(override val baseUrl: String): NetworkProvider {

    private val api by lazy { createRetrofitInstance(baseUrl).create(InternetComputerApi::class.java) }

    suspend fun getBalance(payload: ByteArray): Result<Unit> {
        return try {
            val body = payload.toRequestBody()
            val response = api.makeRequest(IcpSystemCanisters.LEDGER.toString(), "query", body)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

}
